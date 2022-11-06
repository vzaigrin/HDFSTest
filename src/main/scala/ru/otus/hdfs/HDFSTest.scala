package ru.otus.hdfs

import com.typesafe.config.ConfigFactory
import org.apache.hadoop.conf._
import org.apache.hadoop.fs._
import org.apache.hadoop.hdfs.DistributedFileSystem
import org.apache.hadoop.io.IOUtils

object HDFSTest {
  def main(args: Array[String]): Unit = {
    // Читаем конфигурационный файл
    val config           = ConfigFactory.load()
    val from             = config.getString("from")
    val to               = config.getString("to")
    val readySuffix      = config.getString("readySuffix")
    val inProgressSuffix = config.getString("inProgressSuffix")

    // Создаём файловую систему
    val conf             = new Configuration()
    val hdfsCoreSitePath = new Path("core-site.xml")
    val hdfsHDFSSitePath = new Path("hdfs-site.xml")
    conf.addResource(hdfsCoreSitePath)
    conf.addResource(hdfsHDFSSitePath)
    conf.set("fs.hdfs.impl", classOf[DistributedFileSystem].getName)
    conf.set("fs.file.impl", classOf[LocalFileSystem].getName)
    conf.setBoolean("dfs.support.append", true)
    conf.setBoolean("dfs.client.block.write.replace-datanode-on-failure.enable", false)

    val hdfs: FileSystem = FileSystem.get(conf)

    // Если исходный каталог существует
    val fromPath = new Path(from)
    if (hdfs.exists(fromPath))
      try {
        // Читаем исходный каталог
        val listStatus = hdfs.listStatus(fromPath)

        // Проходим по подкаталогам исходного каталога
        listStatus.filter(_.isDirectory).map(_.getPath).foreach { path =>
          val dir = path.getName

          // Получаем список файлов в подкаталоге исходного каталога
          val files = hdfs
            .listStatus(path)
            .filter(_.isFile)
            .map(_.getPath)

          // Подкаталог можно удалять, если нет незаконченных файлов
          val isDirComplete = !files.exists(_.getName.endsWith(inProgressSuffix))

          // Список файлов для копирования - те, которые оканчиваются на readySuffix
          val filesToCopy = files.filter(_.getName.endsWith(readySuffix)).filter(hdfs.getFileStatus(_).getLen > 0)

          // Если есть что копировать
          if (filesToCopy.nonEmpty) {
            // Создаём подкаталоги в целевом каталоге, если их нет
            val newSubDir = s"$to/$dir"
            createFolder(hdfs, newSubDir)

            // Проверяем есть ли в целевом подкаталоге файл, в который надо добавить записи
            // Если есть, будем добавлять в него
            // Если нет, тогда будем добавлять в файл с именем первого файла в исходном подкаталоге
            val fileToAppend =
              (hdfs
                .listStatus(new Path(newSubDir))
                .find(_.getPath.getName.endsWith(readySuffix)) match {
                case Some(name) => name.getPath
                case None       => filesToCopy.head
              }).getName

            val newFile = s"$newSubDir/$fileToAppend"
            val newPath = new Path(newFile)

            // Если в целевом подкаталоге нет файла, в который надо добавлять записи, создаём его
            if (!hdfs.exists(newPath)) hdfs.createNewFile(newPath)

            // Копируем содержимое файлов из исходного подкаталога в целевой подкаталог
            filesToCopy.foreach { fromPath =>
              val inputStream  = hdfs.open(fromPath)
              val outputStream = hdfs.append(newPath)
              IOUtils.copyBytes(inputStream, outputStream, 4096, true)
              inputStream.close()
              outputStream.close()
            }
          }

          // Если в исходном подкаталоге нет незаконченных файлов, удаляем его
          if (isDirComplete) hdfs.delete(path, true)
        }
      } finally if (hdfs != null) hdfs.close()
  }

  def createFolder(fileSystem: FileSystem, folderPath: String): Unit = {
    val path = new Path(folderPath)
    if (!fileSystem.exists(path)) fileSystem.mkdirs(path)
  }
}
