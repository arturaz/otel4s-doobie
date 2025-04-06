import jflex.core.OptionUtils
import jflex.generator.LexGenerator
import sbt.*
import sbt.Keys.*

import java.io.File

object JFlexSbtPlugin extends AutoPlugin {

  object autoImport {
    val jflexInputDirectory = settingKey[File](
      "The directory containing JFlex input files (defaults to src/main/resources/flex)"
    )
    val jflexOutputDirectory = settingKey[File](
      "The directory where generated Java sources will be placed (defaults to sourceManaged in the compile configuration)"
    )
    val jflexTask = taskKey[Seq[File]](
      "Generates Java sources from JFlex input files using LexGenerator"
    )
  }

  import autoImport.*

  override def trigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    jflexInputDirectory := (Compile / sourceDirectory).value / "resources" / "flex",
    jflexOutputDirectory := target.value / "flex",
    jflexTask := {
      val inputDir = jflexInputDirectory.value
      val outputDir = jflexOutputDirectory.value
      val log = streams.value.log

      val inputFiles = (inputDir ** "*.flex").get.toSet

      IO.createDirectory(outputDir)

      val cachedGeneration = FileFunction.cached(
        streams.value.cacheDirectory / "flex",
        inStyle = FilesInfo.lastModified,
        outStyle = FilesInfo.exists
      ) { (in: Set[File]) =>
        in.flatMap { inputFile =>
          // this doesn't seem to work as it's generating under /src/main/resources/flex
          OptionUtils.setDir(outputDir)

          // quick & dirty workaround, copy in target so it generates the java code there
          val targetInputFile = outputDir / inputFile.getName
          IO.copyFile(inputFile, targetInputFile)

          val outputFileName = new LexGenerator(targetInputFile).generate()
          Set(new File(outputFileName))
        }
      }

      cachedGeneration(inputFiles).toSeq
    },
    Compile / unmanagedSourceDirectories += jflexOutputDirectory.value,
    Compile / compile := ((Compile / compile) dependsOn jflexTask).value
  )
}
