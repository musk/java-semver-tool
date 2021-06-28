package com.github.musk.semver;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

public class Semver implements Comparable<Semver>, Serializable {

    final int majorVersion;
    final int minorVersion;
    final int patchVersion;
    private String prerelVersion = null;
    private String buildVersion = null;

    private static final String NAT = "0|[1-9][0-9]*";
    private static final String ALPHANUM = "[0-9]*[A-Za-z-][0-9A-Za-z-]*";
    private static final String IDENT = NAT + "|" + ALPHANUM;
    private static final String FIELD = "[0-9A-Za-z-]+";
    private static final Pattern SEMVER_REGEX = Pattern.compile(
            "^[vV]?(" + NAT + ")\\.(" + NAT + ")\\.(" + NAT + ")(\\-(" + IDENT + ")(\\.(" + IDENT + "))*)?(\\+" + FIELD
                    + "(\\." + FIELD + ")*)?$");

    public Semver(String version) {
        // Matcher groups
        //        0. ALL
        //        1. MAJOR
        //        2. MINOR
        //        3. PATCH
        //        4. PRERELEASE
        //        8. BUILD_METADATA
        var matcher = SEMVER_REGEX.matcher(version);
        if (matcher.matches()) {
            this.majorVersion = Integer.parseInt(matcher.group(1));
            this.minorVersion = Integer.parseInt(matcher.group(2));
            this.patchVersion = Integer.parseInt(matcher.group(3));
            this.prerelVersion = matcher.group(4) != null ? matcher.group(4).substring(1) : null;
            this.buildVersion = matcher.group(8) != null ? matcher.group(8).substring(1) : null;
        } else {
            throw new IllegalArgumentException("Invalid semantic version '" + version + "'");
        }
    }

    private Semver(int major, int minor, int patch) {
        this.majorVersion = major;
        this.minorVersion = minor;
        this.patchVersion = patch;
    }

    public static Semver parse(String version) throws IllegalArgumentException {
        return new Semver(version);
    }

    public static Semver copy(Semver semver) {
        var retVal = new Semver(semver.majorVersion, semver.minorVersion, semver.patchVersion);
        retVal.prerelVersion = semver.prerelVersion;
        retVal.buildVersion = semver.buildVersion;
        return retVal;
    }

    public static boolean validate(String version) {
        try {
            parse(version);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public Semver patch() {
        return bump(Bump.PATCH);
    }

    public Semver minor() {
        return bump(Bump.MINOR);
    }

    public Semver major() {
        return bump(Bump.MAJOR);
    }

    public Semver prerel(String prerel) {
        return new Semver(versionCore() + opt(prerel, "-"));
    }

    public Semver build(String build) {
        return new Semver(versionCore() + opt(prerelVersion, "-") + opt(build, "+"));
    }

    public String text() {
        return versionCore() + opt(prerelVersion, "-") + opt(buildVersion, "+");
    }

    private static String opt(String value, String sep) {
        return value != null && !value.isEmpty() ? (sep + value) : "";
    }

    public Semver release() {
        return new Semver(versionCore());
    }

    public int getMajor() {
        return majorVersion;
    }

    public int getMinor() {
        return minorVersion;
    }

    public int getPatch() {
        return patchVersion;
    }

    public String getPrerel() {
        return prerelVersion;
    }

    public String getBuild() {
        return buildVersion;
    }

    @Override
    public int compareTo(Semver v) {
        if (majorVersion != v.majorVersion) {
            return Integer.compare(majorVersion, v.majorVersion);
        } else if (minorVersion != v.minorVersion) {
            return Integer.compare(minorVersion, v.minorVersion);
        } else if (patchVersion != v.patchVersion) {
            return Integer.compare(patchVersion, v.patchVersion);
        } else if (!Objects.equals(prerelVersion, v.prerelVersion)) {
            if (prerelVersion == null) {
                return 1;
            } else if (v.prerelVersion == null) {
                return -1;
            }

            // Precedence for two pre-release versions with the same major, minor, and patch version MUST be determined
            // by comparing each dot separated identifier from left to right until a difference is found as follows:
            String[] lIdentifiers = prerelVersion.split("\\.");
            String[] rIdentifiers = v.prerelVersion.split("\\.");

            // Compare element by element - until differnce is found, but only until the shorter identifiers list is consumed
            for (var i = 0; i < Math.min(lIdentifiers.length, rIdentifiers.length); i++) {
                String l = lIdentifiers[i];
                String r = rIdentifiers[i];
                if (!l.equals(r)) {
                    try {
                        // identifiers consisting of only digits are compared numerically and
                        var leftInt = Integer.parseInt(l);
                        var rightInt = Integer.parseInt(r);
                        return Integer.compare(leftInt, rightInt);
                    } catch (NumberFormatException ex) {
                        // identifiers with letters or hyphens are compared lexically in ASCII sort order.
                        return l.compareTo(r);
                    }
                }
            }
            //Numeric identifiers always have lower precedence than non-numeric identifiers.
            //A larger set of pre-release fields has a higher precedence than a smaller set,
            //if all of the preceding identifiers are equal.
            return Integer.compare(lIdentifiers.length, rIdentifiers.length);
        }

        if (buildVersion == null) {
            if (v.buildVersion != null) {
                return 1;
            }
        } else if (v.buildVersion == null) {
            return -1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return text();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Semver)) {
            return false;
        }
        var semver = (Semver) o;
        return majorVersion == semver.majorVersion && minorVersion == semver.minorVersion
                && patchVersion == semver.patchVersion && Objects.equals(prerelVersion, semver.prerelVersion) && Objects
                .equals(buildVersion, semver.buildVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(majorVersion, minorVersion, patchVersion, prerelVersion, buildVersion);
    }

    private String versionCore() {
        return majorVersion + "." + minorVersion + "." + patchVersion;
    }

    Semver bump(Bump bump) {
        var msg = "Bump ´%s´ is unkown";
        if (bump == null) {
            throw new IllegalArgumentException(String.format(msg, bump));
        }

        Semver result;
        switch (bump) {
            case PATCH:
                if ((prerelVersion != null && prerelVersion.isEmpty()) || (buildVersion != null && buildVersion
                        .isEmpty())) {
                    result = new Semver(majorVersion, minorVersion, patchVersion);
                } else {
                    result = new Semver(majorVersion, minorVersion, patchVersion + 1);
                }
                break;
            case MINOR:
                result = new Semver(majorVersion, minorVersion + 1, 0);
                break;
            case MAJOR:
                result = new Semver(majorVersion + 1, 0, 0);
                break;
            case RELEASE:
                result = new Semver(majorVersion, minorVersion, patchVersion);
                break;
            default:
                throw new IllegalArgumentException(String.format(msg, bump));
        }
        return result;
    }
}
