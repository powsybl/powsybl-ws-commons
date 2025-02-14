package com.powsybl.ws.commons.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class GZipUtilsTest {
    // a dummy class to test
    record Person(String name, int age) {

    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testGZip() throws Exception {
        // --- zip and unzip only a single object --- //
        Person person = new Person("Name", 24);
        String personJson = objectMapper.writeValueAsString(person);

        // zip then unzip
        byte[] zippedContent = GZipUtils.zip(personJson);
        Person resultPerson = GZipUtils.unzip(zippedContent, objectMapper, Person.class);

        // check result
        assertThat(resultPerson).isEqualTo(person);

        // --- zip and unzip a collection of objects --- //
        Person reportInfos1 = new Person("Name1", 24);
        Person reportInfos2 = new Person("Name2", 48);
        List<Person> personList = List.of(reportInfos1, reportInfos2);
        String personListJson = objectMapper.writeValueAsString(personList);

        // zip then unzip
        byte[] zippedList = GZipUtils.zip(personListJson);
        List<Person> resultPersonList = GZipUtils.unzip(zippedList, objectMapper, new TypeReference<>() {
        });

        // check result
        assertThat(resultPersonList).isEqualTo(personList);

        // --- unzip a zipped buffer into a file --- //
        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource(".")).getFile() + "testZip.gzip");
        GZipUtils.unzip(zippedList, file.toPath());

        // --- zip a file into a buffer --- //
        byte[] newZippedList = GZipUtils.zip(file.toPath());

        // check result
        assertThat(newZippedList).isEqualTo(zippedList);
    }
}
