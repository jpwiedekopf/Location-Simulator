package com.ispgr5.locationsimulator.data.storageManager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.github.slugify.Slugify
import com.ispgr5.locationsimulator.R
import com.ispgr5.locationsimulator.domain.model.ConfigComponent
import com.ispgr5.locationsimulator.domain.model.Configuration
import com.ispgr5.locationsimulator.domain.model.SoundConverter
import com.ispgr5.locationsimulator.domain.useCase.ConfigurationUseCases
import com.ispgr5.locationsimulator.presentation.MainActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.joda.time.Instant
import org.joda.time.format.DateTimeFormatterBuilder
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

const val TAG = "ConfStorageManager"
const val EXPORT_INTERNAL_PATH = "exports"
const val AUTHORITY_STORAGE_PROVIDER = "com.ispgr5.locationsimulator.fileprovider"

@Serializable
data class ConfigurationSerializer(
    val name: String,
    val description: String? = null,
    val randomOrderPlayback: Boolean,
    val configurationComponents: List<ConfigComponent>,
    val sounds: List<SoundHelp>
) {
    /**
     * This class helps to import and export Sound Files and maps the Sound name to his base64String
     */
    @Serializable
    data class SoundHelp(val name: String, val base64String: String)

}

/**
 * This class handles the interaction with the local Storage to import and export Configurations
 */
class ConfigurationStorageManager(
    private val mainActivity: MainActivity, private val soundStorageManager: SoundStorageManager
) {

    private val prettyJson by lazy {
        Json {
            prettyPrint = true
        }
    }

    /***********************************************\
     *                                              |
     * This Section is for Export Configurations    |
     *                                              |
     **********************************************/

    /**
     * replace special characters in the configuration name (limiting to ASCII), and limit it to 8 chars, so the filename doesn't get too long
     */
    private fun slugifyConfigurationName(configuration: Configuration) =
        when (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Slugify uses the Optional class, added only in API 24
            true -> Slugify.builder().build().slugify(configuration.name).trim()

            else -> {
                val legalCharRegex = Regex("[0-9A-Za-z ]")
                configuration.name.mapNotNull { c: Char ->
                    when (legalCharRegex.matches(c.toString())) {
                        false -> null
                        else -> if (c == ' ') '-' else c
                    }
                }.joinToString("")
                // remove all characters that are not ASCII
                // 0x20 = Space
            }
        }.take(8).trim().trimEnd('-', '_')


    /**
     * This function shares the configuration using the system sharing actions, so the user
     * can select the target app. This approach avoids needing any kind of storage permission
     */
    fun saveConfigurationToStorage(context: Context, configuration: Configuration) {
        //read current time and date and save it with the filename (so there are no duplicates)
        val dtFormatter = DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd").toFormatter()
        val dateTimeString = dtFormatter.print(Instant.now())
        val outputDir = context.filesDir.resolve(EXPORT_INTERNAL_PATH).also {
            it.mkdirs()
        }
        val outputSlug = slugifyConfigurationName(configuration)
        val outputFile = outputDir.resolve("locsim_${(outputSlug)}_$dateTimeString.json").also {
            it.createNewFile()
            it.writeText(getConfigString(configuration))
        }

        val contentUri = FileProvider.getUriForFile(context, AUTHORITY_STORAGE_PROVIDER, outputFile)
        Log.d(TAG, "Sharing file using content uri: $contentUri")
        val shareIntent = ShareCompat.IntentBuilder(context).setStream(contentUri)
            .setType("text/json").intent.apply {
                action = Intent.ACTION_SEND
                setDataAndType(contentUri, "text/json")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                context.getString(R.string.share_export_using)
            )
        )
    }

    private fun getConfigString(configuration: Configuration): String {
        val serializer = ConfigurationSerializer(
            name = configuration.name,
            description = configuration.description,
            randomOrderPlayback = configuration.randomOrderPlayback,
            configurationComponents = configuration.components,
            sounds = serializeSounds(configuration)
        )
        return prettyJson.encodeToString(serializer)
    }

    private fun serializeSounds(configuration: Configuration): List<ConfigurationSerializer.SoundHelp> {
        val mappedNames = mutableListOf<String>()
        return configuration.components.mapNotNull { confComp ->
            when (confComp) {
                is ConfigComponent.Sound -> {
                    //look up if this sound is already in the soundList
                    if (mappedNames.contains(confComp.source)) {
                        return@mapNotNull null
                    }
                    //safe the sound name and the Base64 String to the soundList
                    val byteArray =
                        File(mainActivity.filesDir, "/Sounds/" + confComp.source).readBytes()
                    val audioAsBase64String =
                        SoundConverter().encodeByteArrayToBase64String(byteArray)
                    return@mapNotNull ConfigurationSerializer.SoundHelp(
                        confComp.source,
                        audioAsBase64String
                    )
                }

                else -> null
            }
        }
    }

    /***********************************************\
     *                                             *
     * This Section is for Import Configurations   *
     *                                             *
    \**********************************************/

    /**
     * The Interface to the Database is stored here after receiving it with "pickFileAndSafeToDatabase" as parameter
     */
    private var configurationUseCases: ConfigurationUseCases? = null

    /**
     * This function opens a file picker and reads the file and store the inner configuration to Database
     */
    fun pickFileAndSafeToDatabase(configurationUseCases: ConfigurationUseCases) {
        this.configurationUseCases = configurationUseCases
        readFile.launch(listOf("application/*").toTypedArray())
    }

    /**
     * This variable lets us read a file that we choose and store the read Configuration into Database
     */
    @OptIn(DelicateCoroutinesApi::class)
    private val readFile =
        //open file picker
        mainActivity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { result: Uri? ->
            result?.let { fileUri ->

                val fileContent: String = try {
                    readFileFromUri(fileUri)
                } catch (exception: Exception) {
                    //TODO tell user there was a problem by reading the file
                    return@let
                }

                val deserialized = Json.decodeFromString<ConfigurationSerializer>(fileContent)

                //Edit the sound names in configuration components List, when the Sound already exist
                val components =
                    editComponentList(deserialized.configurationComponents, deserialized.sounds)

                //Store to Database
                GlobalScope.launch {
                    configurationUseCases?.addConfiguration?.let {
                        it(
                            Configuration(
                                name = deserialized.name,
                                description = deserialized.description ?: "",
                                randomOrderPlayback = deserialized.randomOrderPlayback,
                                components = components
                            )
                        )
                    }
                }
            }
        }

    /**
     * This function looks up the already existing Sound files in the private dir and
     * their Base64 String. When the Sound already exists in fact of the same Base64 String then
     * the reference in Sound.source is renamed else this function safes the Sound file into the
     * private dir
     * @return the component list with renamed sound references if sound already exists
     */
    private fun editComponentList(
        compList: List<ConfigComponent>, soundList: List<ConfigurationSerializer.SoundHelp>
    ): List<ConfigComponent> {
        val compListMutable = compList.toMutableList()
        //look up all sounds in the new configuration
        //TODO: this method has problems when the user tries to import a configuration with an identical...
        //name, but different content. Maybe move over to identifying the sound file via a checksum,
        //not the name. To avoid identical names being present in the configuration screen, the app could
        //handle name collisions by adding a (1) suffix or something.
        for (i in compList.indices) {
            if (compList[i] is ConfigComponent.Sound) {
                val soundComp = compList[i] as ConfigComponent.Sound
                //extract this sound name and find it in the sound list
                val soundNameWithEnding: String = soundComp.source
                val soundHelpObject: ConfigurationSerializer.SoundHelp? =
                    soundList.find { soundHelp -> soundHelp.name == soundNameWithEnding }
                if (soundHelpObject == null) {
                    //TODO tell user that the imported Configuration don't have the right Sounds(soundNameWithEnding)
                    println("the imported Configuration don't have the right Sounds($soundNameWithEnding))")
                }

                //search this Sound in the already existing Sound files
                val soundAlreadyExistHere: String? =
                    soundHelpObject?.let { soundStorageManager.soundAlreadyExist(it.base64String) }

                //if this sound don't exist then write it to the private Folder
                if (soundAlreadyExistHere == null) {
                    val outputStream =
                        FileOutputStream(soundStorageManager.getFileInSoundsDir(soundNameWithEnding))
                    outputStream.write(soundHelpObject?.let {
                        SoundConverter().decodeBase64StringToByteArray(
                            it.base64String
                        )
                    })
                    outputStream.close()
                } else { //they are the same because the Base64 Strings match
                    //don't safe the Sound but rename the reference in the SoundObject

                    compListMutable[i] = soundComp.myCopy(source = soundAlreadyExistHere)
                }
            }
        }
        return compListMutable.toList()
    }

    /**
     * This function reads the File Content from a file behind the given uri
     * @return a uri form a file u want to read
     */
    private fun readFileFromUri(fileUri: Uri): String {
        val stringBuilder = StringBuilder()
        try {
            mainActivity.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }
        } catch (exception: Exception) {
            throw Exception("Error by reading from uri: $fileUri")
        }
        return stringBuilder.toString()
    }

}