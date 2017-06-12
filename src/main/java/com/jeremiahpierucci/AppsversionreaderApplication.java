package com.jeremiahpierucci;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

//@SpringBootApplication
public class AppsversionreaderApplication {

    public static void main(String[] args) throws IOException, ParseException {
        final int START_OF_VERSION_ADJUSTER = 17;

        List<JSONObject> apps = new ArrayList<>();

        //		SpringApplication.run(AppsversionreaderApplication.class, args);
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://app-registry.rei-cloud.com/rs/apps")
                .build();

        Response response = client.newCall(request).execute();
        String body = response.body().string();

        JSONParser parser = new JSONParser();
        JSONArray array = (JSONArray) parser.parse(body);
        for (int i = 0; i < array.size(); i++) {
            JSONObject json = (JSONObject) array.get(i);
            System.out.println(json);
            JSONArray evirons = (JSONArray) json.get("environments");
            for (int j = 0; j < evirons.size(); j++) {
                JSONObject obj = (JSONObject) evirons.get(j);
                System.out.println(obj.get("name"));
                if (obj.get("name").equals("prod")) {
                    apps.add(obj);
                }
            }
        }
        for (JSONObject o : apps) {
            if (o.get("activeUrl") != null) {
                String activeUrl = o.get("activeUrl").toString();

                request = new Request.Builder()
                        .url(activeUrl + "/dependencies")
                        .build();

                OkHttpClient unsafeClient = getUnsafeOkHttpClient();

                try {

                    response = unsafeClient.newCall(request).execute();
                    body = response.body().string();
                    //                    System.out.println(body);
                    int i = body.indexOf("crampon-core:jar");
                    if (i != -1) {
                        int end = body.indexOf(",", i + START_OF_VERSION_ADJUSTER);
                        String version = body.substring(i + START_OF_VERSION_ADJUSTER, end - 1);
                        System.out.println(activeUrl + " : " + version);
                    }
                } catch (Exception e) {
                    System.out.println(activeUrl + " : no version available");
                }

            }

        }

    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                throws
                                CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            OkHttpClient okHttpClient = builder.build();
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
