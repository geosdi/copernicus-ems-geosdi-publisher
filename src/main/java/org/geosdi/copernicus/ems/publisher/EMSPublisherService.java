/**
 MIT License

 Copyright (c) 2017 geoSDI

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
package org.geosdi.copernicus.ems.publisher;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Francesco Izzi - CNR IMAA geoSDI Group
 * @email francesco.izzi@geosdi.org
 */

@Component
@PropertySource("classpath:application.properties")
public class EMSPublisherService {

    private static final Logger log = LoggerFactory.getLogger(EMSPublisherService.class);


    @Value("${ems.mission.id}")
    private String missionID;

    @Value("${ems.downloadFilePath}")
    private String downloadFilePath;

    @Value("${geoserver.rest.url}")
    private String geoserverRestURL;

    @Value("${geoserver.rest.user}")
    private String geoserverRestUser;

    @Value("${geoserver.rest.password}")
    private String geoserverRestPassword;

    @Value("${geoserver.rest.workspace}")
    private String geoserverRestWorkspace;


    public void downloadAndPublishToGeoServer(String file, GeoServerRESTReader reader, GeoServerRESTPublisher publisher) {

        try {
            RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build();
            BasicCookieStore cookieStore = new BasicCookieStore();
            BasicClientCookie cookie = new BasicClientCookie("SESSab217149b7c08d4c413622955e35ec61", "76rtt4WAe6Gw3VLBZtnK3eUuOh4ryRKK6fA7_Rlw-YA");
            cookie.setDomain(".copernicus.eu");
            cookie.setPath("/");
            cookieStore.addCookie(cookie);
            HttpClientContext context = HttpClientContext.create();
            context.setCookieStore(cookieStore);


            CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(globalConfig).setDefaultCookieStore(cookieStore).build();
            HttpPost request = new HttpPost(file);
            request.setHeader("Accept-Encoding" ,"gzip, deflate");
            request.setHeader("Origin" ,"http://emergency.copernicus.eu");
            request.setHeader("Accept-Language" ,"it-IT,it;q=0.8,en-US;q=0.6,en;q=0.4");
            request.setHeader("Upgrade-Insecure-Requests" ,"1");
            request.setHeader("User-Agent" ,"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36");
            request.setHeader("Content-Type" ,"application/x-www-form-urlencoded");
            request.setHeader("Accept" ,"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            request.setHeader("Cache-Control" ,"max-age=0");
            request.setHeader("Referer" ,file);
            request.setHeader("Connection" ,"keep-alive");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("confirmation", "1"));
            nameValuePairs.add(new BasicNameValuePair("op", "+Download+file+^"));
            nameValuePairs.add(new BasicNameValuePair("form_build_id", "form-wfWtSFuhPbanIpxEiVM8LPHnvLF5LEOuakUYLcXkCeI^"));
            nameValuePairs.add(new BasicNameValuePair("form_id", "emsmapping_disclaimer_download_form"));
            request.setEntity(new UrlEncodedFormEntity(nameValuePairs));



            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            String fileName = file.substring(file.lastIndexOf("/")+1,file.lastIndexOf(".zip")+4);


            int responseCode = response.getStatusLine().getStatusCode();

            log.info("Request Url: " + request.getURI());
            log.info("Response Code: " + responseCode);

            log.info("Extracted file name: " + file.substring(file.lastIndexOf("/")+1,file.lastIndexOf(".zip")+4));

            InputStream is = entity.getContent();

            String filePath = getDownloadFilePath() + fileName;
            File savedFile = new File(filePath);
            FileOutputStream fos = new FileOutputStream(savedFile);

            int inByte;
            while ((inByte = is.read()) != -1) {
                fos.write(inByte);
            }

            is.close();
            fos.close();

            client.close();

            log.info("File Download Completed!!!");

            log.info("Start Publishing EMS shp collection to geoserver ...");

            if(!reader.existsWorkspace(getGeoserverRestWorkspace())){
                publisher.createWorkspace(getGeoserverRestWorkspace());
            }

            boolean b = publisher.publishShpCollection("topp", "EMS", savedFile.toURI());

            log.info("Published = " + b);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMissionID() {
        return missionID;
    }

    public String getGeoserverRestURL() {
        return geoserverRestURL;
    }

    public String getGeoserverRestUser() {
        return geoserverRestUser;
    }

    public String getGeoserverRestPassword() {
        return geoserverRestPassword;
    }

    public String getGeoserverRestWorkspace() {
        return geoserverRestWorkspace;
    }

    public String getDownloadFilePath() {
        return downloadFilePath;
    }


}
