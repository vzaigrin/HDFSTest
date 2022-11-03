val suffix = ".csv"
val list = Array(
  ".DS_Store",
  "part-0000.csv",
  "part-0001.csv.inprogress",
  "part-0000.csv.inprogress",
  "part-0000.csv",
  "part-0001.csv",
  "part-0002.csv"
)
list.filter(_.endsWith(suffix))
