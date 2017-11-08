# copernicus-ems-geosdi-publisher
SpringBoot Application for publishing Copernicus EMS Vector open data to GeoServer

Setting application.properties



```properties
app.name=copernicus-ems-geosdi-publisher
app.description=${app.name} is a Spring Boot application

ems.mission.id=EMSR238
ems.downloadFilePath=/Users/fizzi/Desktop/

geoserver.rest.url=http://localhost:8080/geoserver
geoserver.rest.user=admin
geoserver.rest.password=geoserver
geoserver.rest.workspace=geosdi
```


mvn clean install

mvn spring-boot:run
