package com.github.musk.semver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SemverTest {
    @ParameterizedTest
    @DisplayName("Create semver with all parts")
    @CsvSource({
            "0.2.1                 , RELEASE, 0.2.1 , no-op",
            "0.2.1                 , MAJOR  , 1.0.0 , major",
            "1.9.1                 , MAJOR  , 2.0.0 , minor",
            "v0.2.1                , MAJOR  , 1.0.0 , v major",
            "V0.2.1                , MAJOR  , 1.0.0 , V major",
            "0.2.1                 , MINOR  , 0.3.0 , minor",
            "1.9.1                 , MINOR  , 1.10.0, patch",
            "0.2.1                 , PATCH  , 0.2.2 , patch",
            "0.2.1-rc1.0           , PATCH  , 0.2.2 , strip pre-release",
            "0.2.1-rc1.0+build-1234, PATCH  , 0.2.2 , strip pre-release and build"
    })
    void createSemverWithAllParts(String version, String level, String result, String details) {
        var semver = new Semver(version).bump(Bump.valueOf(level));
        assertEquals(result, semver.text(), details);
    }

    @ParameterizedTest
    @DisplayName("Bump prerelease")
    @CsvSource({"0.2.1         , rc.1 , 0.2.1-rc.1 , add prerel",
                "0.2.1-0.2+b13 , rc.1 , 0.2.1-rc.1 , replace and strip build metadata",
                "0.2.1+b13     , rc.1 , 0.2.1-rc.1 , strip build metadata"})
    void bumpPrerelease(String version, String prerel, String result, String details) {
        var semver = new Semver(version).prerel(prerel);
        assertEquals(result, semver.text(), details);
    }

    @ParameterizedTest
    @DisplayName("Throw IllegalArgumentException on wrong prerel")
    @CsvSource({"1.0.0, x.7.z.092",
                "1.0.0, x.=.z.92",
                "1.0.0, x.7.z..92",
                "1.0.0, .x.7.z.92",
                "1.0.0, x.7.z.92."})
    void throwIllegalArgumentExceptionOnWrongPrerel(String version, String prerel) {
        // given
        var semver = new Semver(version);
        // then
        assertThrows(IllegalArgumentException.class, () -> semver.prerel(prerel), "Prerel:" + prerel);
    }

    @ParameterizedTest
    @DisplayName("Bump build version")
    @CsvSource({"0.2.1+b13      , b.1       , 0.2.1+b.1       , replace build metadata",
                "0.2.1-rc12+b13 , b.1       , 0.2.1-rc12+b.1  , preserve prerel, replace build metadata",
                "1.0.0          , x.7.z.092 , 1.0.0+x.7.z.092 , attach build metadata"})
    void bumpBuildVersion(String version, String build, String result, String details) {
        var semver = new Semver(version).build(build);
        assertEquals(result, semver.text(), details);
    }

    @ParameterizedTest
    @DisplayName("Bump build version fails with IllegalArgumentException")
    @CsvSource({"1.0.0  , x.=.z.92  , bump invalid character in build-metadata: x.=.z.92",
            "1.0.0  , x.7.z..92 , bump invalid character in build-metadata: x.7.z..92",
            "1.0.0  , .x.7.z.92 , bump invalid character in build-metadata: .x.7.z.92",
            "1.0.0  , x.7.z.92. , bump invalid character in build-metadata: ",
            "1.0.0  , 7.z\\$.92  , bump invalid character in build-metadata: 7.z\\$.92",
            "1.0.0  , 7.z.92._  , bump invalid character in build-metadata: 7.z.92._",
            "1.0.0  , 7.z..92   , bump empty identifier in build-metadata (embedded)",
            "1.0.0  , .x.7.z.92 , bump empty identifier in build-metadata (leading)",
            "1.0.0  , z.92.     , bump empty identifier in build-metadata (trailing)"})
    void bumpBuildVersionFailsWithIllegalArgumentException(String version, String build, String details) {
        var semver = Semver.parse(version);
        assertThrows(IllegalArgumentException.class, () -> semver.build(build), details);
    }

    @Test
    @DisplayName("Get Major")
    void getMajor() {
        var semver = new Semver("0.2.1-rc1.0+build-1234");
        assertEquals(0, semver.getMajor());
    }

    @Test
    @DisplayName("Get Minor")
    void getMinor() {
        var semver = new Semver("0.2.1-rc1.0+build-1234");
        assertEquals(2, semver.getMinor());
    }

    @Test
    @DisplayName("Get Patch")
    void getPatch() {
        var semver = new Semver("0.2.1-rc1.0+build-1234");
        assertEquals(1, semver.getPatch());
    }

    @ParameterizedTest
    @DisplayName("Get prerel")
    @CsvSource({"0.2.1-rc1.0+build-1234 , rc1.0",
                "1.0.0-alpha            , alpha",
                "1.0.0-alpha.1          , alpha.1",
                "1.0.0-0alpha.1         , 0alpha.1",
                "1.0.0-0.3.7            , 0.3.7",
                "1.0.0-x.7.z.92         , x.7.z.92",
                "1.0.0-x-.7.--z.92-     , x-.7.--z.92-"})
    void getPrerel(String version, String prerel) {
        var semver = new Semver(version);
        assertEquals(prerel, semver.getPrerel());
    }

    @ParameterizedTest
    @DisplayName("Get build")
    @CsvSource({"0.2.1-rc1.0+build-1234     , build-1234",
                "1.0.0-alpha+001            , 001",
                "1.0.0+20130313144700       , 20130313144700",
                "1.0.0-beta+exp.sha.5114f85 , exp.sha.5114f85",
                "1.0.0+exp.sha.5114f85      , exp.sha.5114f85",
                "1.0.0-x.7.z.92+02          , 02",
                "1.0.0-x.7.z.92+-alpha-2    , -alpha-2",
                "1.0.0-x.7.z.92+-alpha-2-   , -alpha-2-"})
    void getBuild(String version, String build) {
        var semver = new Semver(version);
        assertEquals(build, semver.getBuild());
    }

    @Test
    @DisplayName("Get release")
    void getRelease() {
        var semver = new Semver("0.2.1-rc1.0+build-1234").release();
        assertEquals("0.2.1", semver.text());
    }

    @Test
    @DisplayName("Bump major")
    void bumpMajor() {
        var semver = new Semver("1.2.3").major();
        assertEquals(2, semver.getMajor());
        assertEquals("2.0.0", semver.text());
    }

    @Test
    @DisplayName("Bump minor")
    void bumpMinor() {
        var semver = new Semver("1.2.3").minor();
        assertEquals(3, semver.getMinor());
        assertEquals("1.3.0", semver.text());
    }

    @Test
    @DisplayName("Bump patch")
    void bumpPatch() {
        var semver = new Semver("1.2.3").patch();
        assertEquals(4, semver.getPatch());
        assertEquals("1.2.4", semver.text());
    }

    @Test
    @DisplayName("Bump null throws IllegalArgumentException")
    void bumpNullThrowsIllegalArgumentException() {
        var semver = Semver.parse("1.2.3");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            semver.bump(null);
        });
        assertEquals("Bump ´null´ is unkown", ex.getMessage());
    }

    @Test
    @DisplayName("Bump release")
    void bumpRelease() {
        var semver = new Semver("1.2.3-SNAPSHOT").bump(Bump.RELEASE);
        assertEquals("1.2.3", semver.text());
    }

    @ParameterizedTest
    @DisplayName("Test message on throwing IllegalArgumentException")
    @CsvSource({"Invalid semantic version 'foo', foo",
                "Invalid semantic version '1.2.', 1.2.",
                "Invalid semantic version '1.2.4-', 1.2.4-",
                "Invalid semantic version '1.2.4+', 1.2.4+"})
    void testMessageOnThrowingIllegalArgumentException (String message, String version) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Semver(version));
        assertEquals(message, ex.getMessage());
    }

    @ParameterizedTest
    @DisplayName("Validate returns false on invalid version")
    @CsvSource({"1."           ,
                "1.2"              ,
                ".2.3"             ,
                "01.9.1"           ,
                "1.09.1"           ,
                "1.9.01"           ,
                "1.9.00"           ,
                "1.9a.0"           ,
                "-1.9.0"           ,
                "1.0.0-x.7.z\\$.92" ,
                "1.0.0-x_.7.z.92"  ,
                "1.0.0-x.7.z.092"  ,
                "1.0.0-x.07.z.092" ,
                "1.0.0-x.7.z..92"  ,
                "1.0.0-.x.7.z.92"  ,
                "1.0.0-x.7.z.92."  ,
                "1.0.0-x+7.z\\$.92" ,
                "1.0.0-x+7.z.92._" ,
                "1.0.0+7.z\\$.92"   ,
                "1.0.0-x+7.z..92"  ,
                "1.0.0+.x.7.z.92"  ,
                "1.0.0-x.7+z.92."
    })
    void validateReturnsFalseOnInvalidVersion(String version) {
        assertFalse(Semver.validate(version), "Version: " + version);
    }

    @ParameterizedTest
    @DisplayName("Validate returns true on valid version")
    @CsvSource({"0.0.0, 1.2.3-alpha01"})
    void validateReturnsTrueOnValidVersion(String version) {
        assertTrue(Semver.validate(version), "Version: " +version);
    }

    @ParameterizedTest
    @DisplayName("Comparision works as expected")
    @CsvSource({
            "1.2.3             , <      , 2.2.3",
            "1.0.0-alpha       , <      , 1.0.0-alpha.1",
            "1.0.0-alpha.1     , <      , 1.0.0-alpha.beta",
            "1.0.0-alpha.beta  , <      , 1.0.0-beta",
            "1.0.0-beta        , <      , 1.0.0-beta.2",
            "1.0.0-beta.2      , <      , 1.0.0-beta.11",
            "1.0.0-beta.2.4    , >      , 1.0.0-beta.2.3",
            "1.0.0-beta.2.4    , <      , 1.0.0-beta.2.4.0",
            "1.0.0-beta.2.ab   , <      , 1.0.0-beta.2.ab.0",
            "1.0.0-beta.2.ab.1 , >      , 1.0.0-beta.2.ab.0",
            "1.0.0-beta.11     , <      , 1.0.0-rc.1",
            "1.0.0-rc.1        , <      , 1.0.0",
            "1.0.0             , >      , 1.0.0-rc.1",
            "1.0.0-alpha       , >      , 1.0.0-666",
            "1.0.0             , =      , 1.0.0",
            "1.0.1             , >      , 1.0.0-rc1",
            "1.0.0-beta2       , >      , 1.0.0-beta11",
            "1.0.0-2           , <      , 1.0.0-11",
            "1.0.0-beta1+a     , <      , 1.0.0-beta2+z",
            "1.0.0-beta2+x     , =      , 1.0.0-beta2+y",
            "1.0.0-12.beta2+x  , >      , 1.0.0-11.beta2+y",
            "1.0.0+x           , =      , 1.0.0+y",
            "0.2.1             , <      , 0.2.2",
            "1.2.1             , =      , 1.2.1",
            "0.3.1             , >      , 0.2.5",
            "1.0.0+hash        , <      , 1.0.0"})
    void comparisionWorksAsExpected(String v1, String operator, String v2) {
        var left = Semver.parse(v1);
        var right = Semver.parse(v2);
        switch (operator) {
            case "<":
                assertTrue(left.compareTo(right) <= -1 && right.compareTo(left) >= 1, left + " < " + right);
                break;
            case ">":
                assertTrue(left.compareTo(right) >= 1 && right.compareTo(left) <= -1, left + " > " + right);
                break;
            case "=":
                assertTrue(left.compareTo(right) == 0 && right.compareTo(left) == 0, left + " = " + right);
                break;
            default:
                throw new IllegalArgumentException("Unknown operator '" + operator + "'");
        }
    }

    @Test
    @DisplayName("Equals for same object")
    void equalsForSameObject() {
        Semver semver = Semver.parse("1.2.3");
        assertEquals(semver, semver);
    }

    @Test
    @DisplayName("Equals same value")
    void equalsSameValue() {
        var v1 = new Semver("1.2.3");
        var v2= new Semver("1.2.3");
        assertEquals(v1, v2);
        assertEquals(v2, v1);
    }

    @Test
    @DisplayName("Test copy")
    void testClone() throws CloneNotSupportedException {
        var v1 = Semver.parse("1.2.3-rc1+abc");
        Semver v2 = Semver.copy(v1);

        assertNotSame(v1, v2);
        assertEquals(v1, v2);
        assertEquals("1.2.3-rc1+abc", v2.text());
    }

    @Test
    @DisplayName("Test toString")
    void testToString() {
        var v1 = Semver.parse("1.2.3-rc1+abc");
        assertEquals(v1.text(), v1.toString());
    }
}
