package tk.wasdennnoch.androidn_ify.extracted.android.pm;

/**
 * Created by victo on 5/5/2017.
 */

public class PackageManager {
    
    public static final int INSTALL_SUCCEEDED = 1;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the package is already installed.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_ALREADY_EXISTS = -1;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the package archive file is invalid.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_INVALID_APK = -2;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the URI passed in is invalid.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_INVALID_URI = -3;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the package manager service found that
     * the device didn't have enough storage space to install the app.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_INSUFFICIENT_STORAGE = -4;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if a package is already installed with
     * the same name.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_DUPLICATE_PACKAGE = -5;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the requested shared user does not
     * exist.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_NO_SHARED_USER = -6;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if a previously installed package of the
     * same name has a different signature than the new package (and the old
     * package's data was not removed).
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_UPDATE_INCOMPATIBLE = -7;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package is requested a shared
     * user which is already installed on the device and does not have matching
     * signature.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_SHARED_USER_INCOMPATIBLE = -8;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package uses a shared library
     * that is not available.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_MISSING_SHARED_LIBRARY = -9;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package uses a shared library
     * that is not available.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_REPLACE_COULDNT_DELETE = -10;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package failed while
     * optimizing and validating its dex files, either because there was not
     * enough storage or the validation failed.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_DEXOPT = -11;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package failed because the
     * current SDK version is older than that required by the package.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_OLDER_SDK = -12;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package failed because it
     * contains a content provider with the same authority as a provider already
     * installed in the system.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_CONFLICTING_PROVIDER = -13;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package failed because the
     * current SDK version is newer than that required by the package.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_NEWER_SDK = -14;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package failed because it has
     * specified that it is a test-only package and the caller has not supplied
     * the {#INSTALL_ALLOW_TEST} flag.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_TEST_ONLY = -15;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the package being installed contains
     * native code, but none that is compatible with the device's CPU_ABI.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_CPU_ABI_INCOMPATIBLE = -16;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package uses a feature that is
     * not available.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_MISSING_FEATURE = -17;
    // ------ Errors related to sdcard
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if a secure container mount point
     * couldn't be accessed on external media.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_CONTAINER_ERROR = -18;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package couldn't be installed
     * in the specified install location.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_INVALID_INSTALL_LOCATION = -19;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package couldn't be installed
     * in the specified install location because the media is not available.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_MEDIA_UNAVAILABLE = -20;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package couldn't be installed
     * because the verification timed out.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_VERIFICATION_TIMEOUT = -21;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package couldn't be installed
     * because the verification did not succeed.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_VERIFICATION_FAILURE = -22;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the package changed from what the
     * calling program expected.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_PACKAGE_CHANGED = -23;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package is assigned a
     * different UID than it previously held.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_UID_CHANGED = -24;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the new package has an older version
     * code than the currently installed package.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_VERSION_DOWNGRADE = -25;
    /**
     * Installation return code: this is passed to the
     * {IPackageInstallObserver} if the old package has target SDK high
     * enough to support runtime permission and the new package has target SDK
     * low enough to not support runtime permissions.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_PERMISSION_MODEL_DOWNGRADE = -26;
    /**
     * Installation parse return code: this is passed to the
     * {IPackageInstallObserver} if the parser was given a path that is
     * not a file, or does not end with the expected '.apk' extension.
     *
     * @hide
     */
    
    public static final int INSTALL_PARSE_FAILED_NOT_APK = -100;
    /**
     * Installation parse return code: this is passed to the
     * {IPackageInstallObserver} if the parser was unable to retrieve the
     * AndroidManifest.xml file.
     *
     * @hide
     */
    
    public static final int INSTALL_PARSE_FAILED_BAD_MANIFEST = -101;
    /**
     * Installation parse return code: this is passed to the
     * {IPackageInstallObserver} if the parser encountered an unexpected
     * exception.
     *
     * @hide
     */
    
    public static final int INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION = -102;
    /**
     * Installation parse return code: this is passed to the
     * {IPackageInstallObserver} if the parser did not find any
     * certificates in the .apk.
     *
     * @hide
     */
    
    public static final int INSTALL_PARSE_FAILED_NO_CERTIFICATES = -103;
    /**
     * Installation parse return code: this is passed to the
     * {IPackageInstallObserver} if the parser found inconsistent
     * certificates on the files in the .apk.
     *
     * @hide
     */
    
    public static final int INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES = -104;
    /**
     * Installation parse return code: this is passed to the
     * {IPackageInstallObserver} if the parser encountered a
     * CertificateEncodingException in one of the files in the .apk.
     *
     * @hide
     */
    
    public static final int INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING = -105;
    /**
     * Installation parse return code: this is passed to the
     * {IPackageInstallObserver} if the parser encountered a bad or
     * missing package name in the manifest.
     *
     * @hide
     */
    
    public static final int INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME = -106;
    /**
     * Installation parse return code: this is passed to the
     * {IPackageInstallObserver} if the parser encountered a bad shared
     * user id name in the manifest.
     *
     * @hide
     */
    
    public static final int INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID = -107;
    /**
     * Installation parse return code: this is passed to the
     * {IPackageInstallObserver} if the parser encountered some structural
     * problem in the manifest.
     *
     * @hide
     */
    
    public static final int INSTALL_PARSE_FAILED_MANIFEST_MALFORMED = -108;
    /**
     * Installation parse return code: this is passed to the
     * {IPackageInstallObserver} if the parser did not find any actionable
     * tags (instrumentation or application) in the manifest.
     *
     * @hide
     */
    
    public static final int INSTALL_PARSE_FAILED_MANIFEST_EMPTY = -109;
    /**
     * Installation failed return code: this is passed to the
     * {IPackageInstallObserver} if the system failed to install the
     * package because of system issues.
     *
     * @hide
     */
    
    public static final int INSTALL_FAILED_INTERNAL_ERROR = -110;
    /**
     * Installation failed return code: this is passed to the
     * {IPackageInstallObserver} if the system failed to install the
     * package because the user is restricted from installing apps.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_USER_RESTRICTED = -111;
    /**
     * Installation failed return code: this is passed to the
     * {IPackageInstallObserver} if the system failed to install the
     * package because it is attempting to define a permission that is already
     * defined by some existing package.
     * <p>
     * The package name of the app which has already defined the permission is
     * passed to a {PackageInstallObserver}, if any, as the
     * {#EXTRA_FAILURE_EXISTING_PACKAGE} string extra; and the name of the
     * permission being redefined is passed in the
     * {#EXTRA_FAILURE_EXISTING_PERMISSION} string extra.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_DUPLICATE_PERMISSION = -112;
    /**
     * Installation failed return code: this is passed to the
     * {IPackageInstallObserver} if the system failed to install the
     * package because its packaged native code did not match any of the ABIs
     * supported by the system.
     *
     * @hide
     */
    public static final int INSTALL_FAILED_NO_MATCHING_ABIS = -113;
    /**
     * Internal return code for NativeLibraryHelper methods to indicate that the package
     * being processed did not contain any native code. This is placed here only so that
     * it can belong to the same value space as the other install failure codes.
     *
     * @hide
     */
    public static final int NO_NATIVE_LIBRARIES = -114;
    /** {@hide} */
    public static final int INSTALL_FAILED_ABORTED = -115;
    /**
     * Installation failed return code: ephemeral app installs are incompatible with some
     * other installation flags supplied for the operation; or other circumstances such
     * as trying to upgrade a system app via an ephemeral install.
     * @hide
     */
    public static final int INSTALL_FAILED_EPHEMERAL_INVALID = -116;
}
