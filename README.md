# Знакомство с HDFS

## Задача
Требуется написать Scala-приложение, которое будет очищать данные из папки */stage* и складывать их в папку */ods* в корне HDFS по следующим правилам:

* Структура партиций (папок вида *date=...*) должна сохраниться.
* Внутри папок должен остаться только один файл, содержащий все данные файлов из соответствующей партиции в папке */stage*.

То есть, если у нас есть папка */stage/date=2020-11-11* с файлами
```
part-0000.csv
part-0001.csv
```

то должна получиться папка */ods/date=2020-11-11* с одним файлом
> part-0000.csv

содержащим все данные из файлов папки */stage/date=2020-11-11*

## Подготовка инфраструктуры
* Добавьте в файл */etc/hosts* следующие записи для возможности работать с Hadoop локально
```
 127.0.0.1 namenode
 127.0.0.1 datanode
```
* Запустите контейнеры, выполнив команду *docker-compose up -d*
* Скопируйте данные в HDFS
```
docker exec namenode hdfs dfs -put /sample_data/stage /
```

## Запуск
* Посмотрим на исходные файлы в HDFS:
```
docker exec namenode hdfs dfs -ls -R /
drwxr-xr-x   - root supergroup          0 2022-11-06 15:32 /stage
drwxr-xr-x   - root supergroup          0 2022-11-06 15:32 /stage/date=2020-12-01
-rw-r--r--   3 root supergroup       6148 2022-11-06 15:32 /stage/date=2020-12-01/.DS_Store
-rw-r--r--   3 root supergroup          0 2022-11-06 15:32 /stage/date=2020-12-01/part-0000.csv
-rw-r--r--   3 root supergroup        595 2022-11-06 15:32 /stage/date=2020-12-01/part-0001.csv.inprogress
drwxr-xr-x   - root supergroup          0 2022-11-06 15:32 /stage/date=2020-12-02
-rw-r--r--   3 root supergroup       1736 2022-11-06 15:32 /stage/date=2020-12-02/part-0000.csv.inprogress
drwxr-xr-x   - root supergroup          0 2022-11-06 15:32 /stage/date=2020-12-03
-rw-r--r--   3 root supergroup       2588 2022-11-06 15:32 /stage/date=2020-12-03/part-0000.csv
-rw-r--r--   3 root supergroup        633 2022-11-06 15:32 /stage/date=2020-12-03/part-0001.csv
-rw-r--r--   3 root supergroup       1329 2022-11-06 15:32 /stage/date=2020-12-03/part-0002.csv
```
* Запустим приложение первый раз
> java -jar HDFSTest-assembly-1.0.jar
* Посмотрим, что получилось
```
docker exec namenode hdfs dfs -ls -R /
drwxr-xr-x   - vadim supergroup          0 2022-11-06 15:42 /ods
drwxr-xr-x   - vadim supergroup          0 2022-11-06 15:42 /ods/date=2020-12-03
-rw-r--r--   3 vadim supergroup       4550 2022-11-06 15:42 /ods/date=2020-12-03/part-0000.csv
drwxr-xr-x   - root  supergroup          0 2022-11-06 15:42 /stage
drwxr-xr-x   - root  supergroup          0 2022-11-06 15:32 /stage/date=2020-12-01
-rw-r--r--   3 root  supergroup       6148 2022-11-06 15:32 /stage/date=2020-12-01/.DS_Store
-rw-r--r--   3 root  supergroup          0 2022-11-06 15:32 /stage/date=2020-12-01/part-0000.csv
-rw-r--r--   3 root  supergroup        595 2022-11-06 15:32 /stage/date=2020-12-01/part-0001.csv.inprogress
drwxr-xr-x   - root  supergroup          0 2022-11-06 15:32 /stage/date=2020-12-02
-rw-r--r--   3 root  supergroup       1736 2022-11-06 15:32 /stage/date=2020-12-02/part-0000.csv.inprogress
```
* Файл */stage/date=2020-12-01/part-0000.csv* не скопировался потому, что у него нулевая длина.
* Файлы */stage/date=2020-12-01/part-0001.csv.inprogress* и */stage/date=2020-12-02/part-0000.csv.inprogress* не скопировались потому, что *.inprogress* - это признак того, что файл находится в обработке и его копировать не надо.
* Переименуем файлы, находящиеся в обработке так, будто бы обработка закончилась
```
docker exec namenode hdfs dfs -mv /stage/date=2020-12-01/part-0001.csv.inprogress /stage/date=2020-12-01/part-0001.csv
docker exec namenode hdfs dfs -mv /stage/date=2020-12-02/part-0000.csv.inprogress /stage/date=2020-12-02/part-0000.csv
```
* Посмотрим, что получилось
```
docker exec namenode hdfs dfs -ls -R /
drwxr-xr-x   - vadim supergroup          0 2022-11-06 15:42 /ods
drwxr-xr-x   - vadim supergroup          0 2022-11-06 15:42 /ods/date=2020-12-03
-rw-r--r--   3 vadim supergroup       4550 2022-11-06 15:42 /ods/date=2020-12-03/part-0000.csv
drwxr-xr-x   - root  supergroup          0 2022-11-06 15:42 /stage
drwxr-xr-x   - root  supergroup          0 2022-11-06 15:44 /stage/date=2020-12-01
-rw-r--r--   3 root  supergroup       6148 2022-11-06 15:32 /stage/date=2020-12-01/.DS_Store
-rw-r--r--   3 root  supergroup          0 2022-11-06 15:32 /stage/date=2020-12-01/part-0000.csv
-rw-r--r--   3 root  supergroup        595 2022-11-06 15:32 /stage/date=2020-12-01/part-0001.csv
drwxr-xr-x   - root  supergroup          0 2022-11-06 15:44 /stage/date=2020-12-02
-rw-r--r--   3 root  supergroup       1736 2022-11-06 15:32 /stage/date=2020-12-02/part-0000.csv
```
* Запустим приложение ещё раз
> java -jar HDFSTest-assembly-1.0.jar
* Посмотрим, что получилось
```
docker exec namenode hdfs dfs -ls -R /
drwxr-xr-x   - vadim supergroup          0 2022-11-06 15:44 /ods
drwxr-xr-x   - vadim supergroup          0 2022-11-06 15:44 /ods/date=2020-12-01
-rw-r--r--   3 vadim supergroup        595 2022-11-06 15:44 /ods/date=2020-12-01/part-0001.csv
drwxr-xr-x   - vadim supergroup          0 2022-11-06 15:44 /ods/date=2020-12-02
-rw-r--r--   3 vadim supergroup       1736 2022-11-06 15:44 /ods/date=2020-12-02/part-0000.csv
drwxr-xr-x   - vadim supergroup          0 2022-11-06 15:42 /ods/date=2020-12-03
-rw-r--r--   3 vadim supergroup       4550 2022-11-06 15:42 /ods/date=2020-12-03/part-0000.csv
drwxr-xr-x   - root  supergroup          0 2022-11-06 15:44 /stage
```
* Задача решена