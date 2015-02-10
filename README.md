# CLI Tools to Handle Event Streams

## AppendLog

This tool can be used to append two logs, one after the other, in order to generate another log. Example usage:
```shell
$ java -jar AppendLogs.jar log1.xes log2.xes destination-log.xes
```

## LogToStream

This tool can be used to transform a standard log file into a stream file (i.e., a file with one event per line, chronologically sorted). Example usage:
```shell
$ java -jar LogToStream.jar log.xes target.stream true
```
The last parameter (either `true` or `false`) indicates whether the tool has to tag the first and last event of each trace.
