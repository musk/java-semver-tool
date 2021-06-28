The semver java utility
=========================

This is a Java port of [groovy-semver-tool](https://github.com/Wonno/groovy-semver-tool) 

License [GNU LGPL v3.0](https://github.com/musk/java-semver-tool/blob/main/LICENSE)

semver is a little tool to manipulate the version bumping in a project that follows the [Semver Specification 2.0](https://semver.org/spec/v2.0.0.html) .

Its use are:
  - bump version
  - extract specific version part
  - compare versions

A  version must match the following regular expression:
```
^[vV]?(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(\-(0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*)(\.(0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*))*)?(\+[0-9A-Za-z-]+(\.[0-9A-Za-z-]+)*)?$
```

In English:
- The version must match _X.Y.Z[-PRERELEASE][+BUILD]_ where _X_, _Y_ and _Z_ are non-negative integers.
- _PRERELEASE_ is a dot separated sequence of non-negative integers and/or identifiers composed of alphanumeric 
  characters and hyphens (with at least one non-digit). Numeric identifiers must not have leading zeros. A hyphen 
  (\"-\") introduces this optional part.
- _BUILD_ is a dot separated sequence of identifiers composed of alphanumeric characters and hyphens. A plus ("+") 
  introduces this optional part.

## Build
```
mvn clean install
```
     
## Examples
```$Java
import com.github.musk.semver.Semver

//version validation
assert !Semver.validate("1.2.invalid")

//version change
var version = Semver.parse("1.2.3+abcd").prerel("rc1").minor().text()
assert Objects.equals(version, "1.3.0")

//version comparison v1 < v2
var v1 = Semver.parse("1.0.7+acf430")
var v2 = new Semver("1.0.6").patch()
assert v1.compareTo(v2) == -1
```

## Links
* [Semver Specification 2.0](https://semver.org/spec/v2.0.0.html) 
* Inspired by [semver-tool](https://github.com/fsaintjacques/semver-tool/) written in bash.

## Credits
* ported from [groovy-semver-tool](https://github.com/Wonno/groovy-semver-tool)
* [semver-tool](https://github.com/fsaintjacques/semver-tool/) project for the regex and the testcases
