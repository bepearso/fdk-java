package com.fnproject.fn.testing;

import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.InputEvent;
import com.fnproject.fn.api.OutputEvent;
import com.fnproject.fn.api.RuntimeContext;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.requests.ListRegionsRequest;
import com.oracle.bmc.identity.responses.ListRegionsResponse;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

public class ResourcePrincipalTest {

    public static Map<String, String> configuration;
    public static InputEvent inEvent;
    public static List<InputEvent> capturedInputs = new ArrayList<>();
    public static List<byte[]> capturedBodies = new ArrayList<>();


    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();
    private final String exampleBaseUrl = "http://www.example.com";

    @Before
    public void reset() {
        fn.addSharedClass(ResourcePrincipalTest.class);
        fn.addSharedClass(ResourcePrincipalAuthenticationDetailsProvider.class);
        fn.addSharedClass(InputEvent.class);


        ResourcePrincipalTest.configuration = null;
        ResourcePrincipalTest.inEvent = null;
        ResourcePrincipalTest.capturedInputs = new ArrayList<>();
        ResourcePrincipalTest.capturedBodies = new ArrayList<>();
    }


    public static class TestFn {
        private RuntimeContext ctx;

        public TestFn(RuntimeContext ctx) {
            this.ctx = ctx;
        }

        public void copyConfiguration() {
            configuration = new HashMap<>(ctx.getConfiguration());
        }

        public void copyInputEvent(InputEvent inEvent) {
            FnTestingRuleTest.inEvent = inEvent;
        }

        public void err() {
            throw new RuntimeException("ERR");
        }

        public void captureInput(InputEvent in) {
            capturedInputs.add(in);
            capturedBodies.add(in.consumeBody(TestFn::consumeToBytes));
        }

        private static byte[] consumeToBytes(InputStream is) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                IOUtils.copy(is, bos);
                return bos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        public OutputEvent echoInput(InputEvent in) {
            byte[] result = in.consumeBody(TestFn::consumeToBytes);
            return OutputEvent.fromBytes(result, OutputEvent.Status.Success, "application/octet-stream");
        }

    }


    public static class TestFunc{

        public static void test(){
            // --------------------- Resource Principals Provider ---------------------
            final ResourcePrincipalAuthenticationDetailsProvider provider;
            ResourcePrincipalAuthenticationDetailsProvider.ResourcePrincipalAuthenticationDetailsProviderBuilder builder = ResourcePrincipalAuthenticationDetailsProvider.builder();

            provider = builder.build();
            // --------------------- Resource Principals Provider ---------------------

//        final AuthenticationDetailsProvider provider;
//        try {
//            provider = new ConfigFileAuthenticationDetailsProvider("DEFAULT");


            final IdentityClient identityClient = new IdentityClient(provider);
            identityClient.setRegion(Region.US_PHOENIX_1);

            System.out.println("Querying for list of regions via the Identity Client");
            final ListRegionsResponse response =
                    identityClient.listRegions(ListRegionsRequest.builder().build());
            System.out.println("List of regions: " + response.getItems());
        }
    }

    @Test
    public void RPTest() throws Exception {


        System.err.println(ResourcePrincipalAuthenticationDetailsProvider.class.getConstructors());

        //ByteBuddyAgent.install();
        ClassLoader cl = new FnTestingClassLoader(fn.getClass().getClassLoader(), fn.getSharedPrefixes());

        Class<?> clazz = cl.loadClass(ResourcePrincipalAuthenticationDetailsProvider.class.getName());

        Class<?> clazz2 = cl.loadClass(ResourcePrincipalAuthenticationDetailsProvider.ResourcePrincipalAuthenticationDetailsProviderBuilder.class.getName());
        System.out.println(clazz2);

        Method buildMethod = clazz.getMethod("builder");

        buildMethod.invoke(null);

        Class<?> testFnClass = cl.loadClass(TestFunc.class.getName());

        testFnClass.getMethod("test").invoke(null);



    }


//        @Test
//        public void shouldLoadClass () {
////        Thread.currentThread().setContextClassLoader(cl);
//
//            ByteBuddyAgent.install();
//            new ByteBuddy()
//                    .redefine(ResourcePrincipalAuthenticationDetailsProvider.class)
//                    .make()
//                    .load(ResourcePrincipalAuthenticationDetailsProvider.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
//            try {
//                ResourcePrincipalAuthenticationDetailsProvider provider = ResourcePrincipalAuthenticationDetailsProvider.builder().build();
//
//            } catch (Exception ex) {
//                System.err.println("Exception in FDK " + ex.getMessage());
//                ex.printStackTrace();
//                throw new RuntimeException("", ex);
//            }
//        }


    private static Map.Entry<String, List<String>> headerEntry(String key, String... values) {
        return new AbstractMap.SimpleEntry<>(Headers.canonicalKey(key), Arrays.asList(values));
    }

}
