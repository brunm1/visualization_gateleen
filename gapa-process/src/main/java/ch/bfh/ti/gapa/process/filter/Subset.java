package ch.bfh.ti.gapa.process.filter;

import ch.bfh.ti.gapa.domain.recording.Record;
import ch.bfh.ti.gapa.domain.recording.Recording;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Subset {
    private List<Predicate<Record>> filters;
    private Recording recording;

    Subset(List<Predicate<Record>> filters, Recording recording) {
        this.filters = filters;
        this.recording = recording;
    }

    /**
     * Applies the filters on the records of the recording
     * @return a list of records
     */
    List<Record> getRecords() {
        Stream<Record> stream = recording.getRecords().stream();
        for(Predicate<Record> filter: filters) {
            stream = stream.filter(filter::test);
        }
        return stream.collect(Collectors.toList());
    }
}
