package com.fnproject.fn.testing;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.ConfigFileReader.ConfigFile;
import com.oracle.bmc.OCID;
import com.oracle.bmc.Realm;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.*;
import com.oracle.bmc.auth.internal.FederationClient;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;


public class FakeAuthenticationDetailsProvider extends AbstractRequestingAuthenticationDetailsProvider implements RegionProvider, RefreshableOnNotAuthenticatedProvider<String> {

    String configFilePath = "~/.oci/config";
    String profile = "DEFAULT";

    //    static String regionId;
    String fingerprint;
    String tenantId;
    String userId;
    String passPhrase;
    Supplier<InputStream> privateKeySupplier;
    String pemFilePath;
    Region region;
    String keyID;



    public FakeAuthenticationDetailsProvider() throws IOException{
        super(null, new SessionKeySupplier() {
                    @Override
                    public KeyPair getKeyPair() {
                        return null;
                    }

                    @Override
                    public RSAPublicKey getPublicKey() {
                        return null;
                    }

                    @Override
                    public RSAPrivateKey getPrivateKey() {
                        return null;
                    }

                    @Override
                    public void refreshKeys() {

                    }
                });

//            ConfigFile configFile = ConfigFileReader.parse(configFilePath, profile);
                ConfigFile configFile = ConfigFileReader.parseDefault(profile);

            this.fingerprint =
                    Preconditions.checkNotNull(
                            configFile.get("fingerprint"), "missing fingerprint in config");
            this.tenantId =
                    Preconditions.checkNotNull(configFile.get("tenancy"), "missing tenancy in config");
            this.userId =
                    Preconditions.checkNotNull(configFile.get("user"), "missing user in config");
            this.pemFilePath =
                    Preconditions.checkNotNull(
                            configFile.get("key_file"), "missing key_file in config");
            // pass phrase is optional
            this.passPhrase = configFile.get("pass_phrase");

            this.privateKeySupplier = new SimplePrivateKeySupplier(pemFilePath);


            // region is optional, for backwards compatibility, if region is not known, log an error and continue.
            // the same file may be used by other tools, where the region can be a newly launched region value
            // that is not supported by the SDK yet.

            String regionId;
            regionId = configFile.get("region");
            if (regionId != null) {
                try {
                    region = Region.fromRegionId(regionId);
                } catch (IllegalArgumentException e) {
                    // Proceed by assuming the region id in the config file belongs to OC1 realm.
                    region = Region.register(regionId, Realm.OC1);
                }
            } else {
                System.err.println("Region not specified in Config file. Proceeding without setting a region.");
            }
    }
//
//    FakeAuthenticationDetailsProvider(FederationClient client, SessionKeySupplier supplier, Region r) {
//        System.err.println("YAYAYYAYAAY");
//
//    }



    public static ResourcePrincipalAuthenticationDetailsProvider.ResourcePrincipalAuthenticationDetailsProviderBuilder builder() {
        try {
            System.err.println("WOOP RUNNING THE FAKE");
            Class<?> builderClas = FakeAuthenticationDetailsProvider.class.getClassLoader().loadClass(ResourcePrincipalAuthenticationDetailsProvider.ResourcePrincipalAuthenticationDetailsProviderBuilder.class.getName());


            Constructor<?> cons =
                    builderClas.getDeclaredConstructor();
            cons.setAccessible(true);
            return (ResourcePrincipalAuthenticationDetailsProvider.ResourcePrincipalAuthenticationDetailsProviderBuilder)cons.newInstance();


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static class FakeBuilder {
        public static  ResourcePrincipalAuthenticationDetailsProvider build() {

            System.out.println("MY RESOURCE PRINCIPAL AUTH PROV");


//            final String tenantId = ResourcePrincipalAuthenticationDetailsProvider.tenantId;
//            final String userId = ResourcePrincipalAuthenticationDetailsProvider.userId;
//            final Supplier<InputStream> privateKeySupplier = ResourcePrincipalAuthenticationDetailsProvider.privateKeySupplier;
//            final String fingerprint = ResourcePrincipalAuthenticationDetailsProvider.fingerprint;
//            final Region region = ResourcePrincipalAuthenticationDetailsProvider.region;

//            final FederationClient federationClient;
//            final SessionKeySupplier sessionKeySupplier;
//            final InputStream ociResourcePrincipalPrivateKey = privateKeySupplier.get();
//            final String ociResourcePrincipalPassphrase = passPhrase;
//
//            if (ociResourcePrincipalPrivateKey == null) {
//                throw new IllegalArgumentException(
//                        "OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM environment variable missing");
//            }
//            if (new File(ociResourcePrincipalPrivateKey.toString()).isAbsolute()) {
//                if (ociResourcePrincipalPassphrase != null
//                        && !new File(ociResourcePrincipalPassphrase).isAbsolute()) {
//                    throw new IllegalArgumentException(
//                            "cannot mix path and constant settings for OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM and OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM_PASSPHRASE");
//                }
//                sessionKeySupplier =
//                        new FileBasedKeySupplier(
//                                ociResourcePrincipalPrivateKey, ociResourcePrincipalPassphrase);
//            } else {
//                final char[] passPhraseChars;
//                if (ociResourcePrincipalPassphrase != null) {
//                    passPhraseChars = ociResourcePrincipalPassphrase.toCharArray();
//                } else {
//                    passPhraseChars = null;
//                }
//                sessionKeySupplier =
//                        new FixedContentKeySupplier(
//                                ociResourcePrincipalPrivateKey, passPhraseChars);
//            }

//            final String ociResourcePrincipalRPST = System.getenv(OCI_RESOURCE_PRINCIPAL_RPST);
//            if (ociResourcePrincipalRPST == null) {
//                throw new IllegalArgumentException(
//                        OCI_RESOURCE_PRINCIPAL_RPST + " environment variable missing");
//            }
//            if (new File(ociResourcePrincipalRPST).isAbsolute()) {
//                federationClient =
//                        new FileBasedResourcePrincipalFederationClient(
//                                sessionKeySupplier, ociResourcePrincipalRPST);
//            } else {
//                federationClient =
//                        new FixedContentResourcePrincipalFederationClient(
//                                ociResourcePrincipalRPST, sessionKeySupplier);
//            }
//
//            final String ociResourcePrincipalRegion = region.toString();
//            if (ociResourcePrincipalRegion == null) {
//                throw new IllegalArgumentException(
//                        "OCI_RESOURCE_PRINCIPAL_REGION environment variable missing");
//            } else {
//                region =
//                        Region.valueOf(
//                                NameUtils.canonicalizeForEnumTypes(ociResourcePrincipalRegion));
//            }

//            return new ResourcePrincipalAuthenticationDetailsProvider();
            try {
                System.err.println("WOOP RUNNING THE FAKE");
                Class<?> builderClas = FakeAuthenticationDetailsProvider.class.getClassLoader().loadClass(ResourcePrincipalAuthenticationDetailsProvider.class.getName());


                Constructor<?> cons =
                        builderCgetDeclaredConstructor(FederationClient.class,SessionKeySupplier.class,Region.class);
                cons.setAccessible(true);
                return (ResourcePrincipalAuthenticationDetailsProvider)cons.newInstance(null,null,null);


            } catch (Exception e) {
                throw new RuntimeException(e);
            }        }
    }


    @Override
    public String refresh() {
        return null;
    }


    public static class ResourcePrincipalAuthenticationDetailsProviderBuilder  {

//        ResourcePrincipalAuthenticationDetailsProviderBuilder() {
//        }


        /**
         * Helper method that interprets the runtime environment to build a v2.2-configured client
         *
         * @return ResourcePrincipalAuthenticationDetailsProvider
         */

       // @Advice.OnMethodEnter
    }

    public String getFingerprint() {
        return fingerprint;
    }


    public String getTenantId() {
        return tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public InputStream getPrivateKey() {
        return privateKeySupplier.get();
    }

    public String getPassPhrase() {
        return passPhrase;
    }

    public char[] getPassphraseCharacters() {
        return new char[0];
    }

    @Override
    public Region getRegion() {
        return region;
    }

    public String getKeyId() {
        if (!OCID.isValid(getTenantId())) {
        }
        if (!OCID.isValid(getUserId())) {
        }

        String keyId = CustomerKeyIdFormatter.createKeyId((AuthenticationDetailsProvider) this);
        return keyId;
    }

}
