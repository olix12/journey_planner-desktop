#! /bin/bash
wget -O gtfs.zip http://transitfeeds.com/p/dakk/625/latest/download
unzip gtfs.zip -d gtfs/

for f in gtfs/*; do sed 1d $f > "$f.2" && mv "$f.2" $f; done

sed 's/\([0-9]\{4\}\)\([0-9]\{2\}\)\([0-9]\{2\}\)/\1-\2-\3/g' gtfs/calendar.txt > gtfs/calendar.txt.2 && mv gtfs/calendar.txt.2 gtfs/calendar.txt

tables="stops routes trips calendar stop_times"
for table in $tables;
do sqlite3 timetable.db ".mode csv" "delete from $table" ".import gtfs/$table.txt $table";
done

rm gtfs.zip
rm -r gtfs
