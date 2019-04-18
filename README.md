# 003664.2019.NDW_DSS_Mapmatching

Deze code hoort bij het project 003664.2019.NDW_DSS_Mapmatching, uitgevoerd door DAT.Mobility voor het NDW in het kader van de Data Science Society.

## Algemene opzet van de code

* Bronnen die met Java geparst worden hebben per bron een Java project met suffix `-parser`:
    * In ieder van deze Java projecten bevat de package op het hoogste niveau een klasse met suffix `Exporter`:
        * Deze klasse is te draaien als Java project in Eclipse en gebruikt een klasse met suffix `Parser` om de data te parsen.
        * De `Exporter` klasse genereert één of meerdere SQL bestanden die handmatig in PostgreSQL te laden zijn, bijvoorbeeld met pgAdmin of via de commandline tool `psql`.
        * Per Java project staat er een specifiekere uitleg onder de volgende kopjes.
* Map matching algoritmes die in Java zijn geschreven staan in Java projecten met suffix `-map-matcher`:
    * In ieder van deze Java projecten bevat de package op het hoogste niveau een klasse met suffix `MapMatcher`:
        * Deze klasse is te draaien als Java project in Eclipse en voert de map matching algoritmes uit voor één of meerdere bronnen.
        * Communicatie met de database gebeurt binnen Java met de klassen onder de package `dataaccess.dao`. In deze klassen staat de SQL (inclusief de gebruikte tabelnamen) om de benodigde data op te halen of weg te schrijven.
        * Per Java project staat er een specifiekere uitleg onder de volgende kopjes.

## mst-parser

De MST parser kan gebruikt worden om data van bijvoorbeeld http://opendata.ndw.nu/measurement.xml.gz te parsen. Voorbeeld van gebruik:

* Installeer Eclipse.
* Importeer de map `mst-parser` als Maven project in Eclipse.
* Download het bestand http://opendata.ndw.nu/measurement.xml.gz en zet het in `mst-parser/files/measurement.xml.gz`.
* Voer binnen Eclipse de klasse `MeasurementSiteTableExporter` uit als Java-applicatie.
* Installeer PostgreSQL 10 en PostGIS 2.5 indien nodig.
* Maak een nieuwe database `ndssmapmatching` (of hergebruik hem als deze database al bestaat).
* Voer de queries van `mst-parser/queries/create.sql` uit in de nieuwe database.
* Voer de queries van `mst-parser/files/measurement.sql` uit in de nieuwe database.
* Voer de queries van `mst-parser/files/locations.sql` uit in de nieuwe database.

## situations-parser

De situations parser kan gebruikt worden om data van bijvoorbeeld http://opendata.ndw.nu/actuele_statusberichten.xml.gz en http://opendata.ndw.nu/wegwerkzaamheden.xml.gz te parsen. Voorbeeld van gebruik:

* Installeer Eclipse.
* Importeer de map `situations-parser` als Maven project in Eclipse.
* Download het bestand http://opendata.ndw.nu/actuele_statusberichten.xml.gz en zet het in `situations-parser/files/actuele_statusberichten.xml.gz`.
* Download het bestand http://opendata.ndw.nu/wegwerkzaamheden.xml.gz en zet het in `situations-parser/files/wegwerkzaamheden.xml.gz`.
* Voer binnen Eclipse de klasse `SituationsExporter` uit als Java-applicatie.
* Installeer PostgreSQL 10 en PostGIS 2.5 indien nodig.
* Maak een nieuwe database `ndssmapmatching` (of hergebruik hem als deze database al bestaat).
* Voer de queries van `situations-parser/queries/create.sql` uit in de nieuwe database.
* Voer de queries van `situations-parser/files/situation_records.sql` uit in de nieuwe database.
* Voer de queries van `situations-parser/files/situation_record_locations.sql` uit in de nieuwe database. Vanwege de grootte kan het best een commando als `psql -f situation_record_locations.sql -p 5432 ndssmapmatching postgres > out.txt` gebruikt worden (`psql` wordt meegeleverd met PostgreSQL).

## waze-parser

De waze parser kan gebruikt worden om waze alerts te parsen. Die zijn in JSON formaat opgeslagen en zijn op te halen via Microsoft Azure Storage Explorer.
Voorbeeld van gebruik:

* Installeer Eclipse.
* Importeer de map `waze-parser` als Maven project in Eclipse.
* Download *.json.gz bestanden met waze alerts via Microsoft Azure Storage Explorer en zet ze in `waze-parser/files_in/`.
* Voer binnen Eclipse de klasse `WazeExporter` uit als Java-applicatie.
* Installeer PostgreSQL 10 en PostGIS 2.5 indien nodig.
* Maak een nieuwe database `ndssmapmatching` (of hergebruik hem als deze database al bestaat).
* Voer de queries van `waze-parser/queries/create.sql` uit in de nieuwe database.
* Voer de queries van `waze-parser/files_out/waze_data.sql` uit in de nieuwe database. Vanwege de grootte kan het best het commando als `psql -f waze_data.sql -p 5432 -d ndssmapmatching -U postgres` gebruikt worden (`psql` wordt meegeleverd met PostgreSQL).

## msi-parser

De MSI parser kan gebruikt worden om data van bijvoorbeeld http://opendata.ndw.nu/Matrixsignaalinformatie.xml.gz te parsen. Voorbeeld van gebruik:

* Installeer Eclipse.
* Importeer de map `msi-parser` als Maven project in Eclipse.
* Download het bestand http://opendata.ndw.nu/Matrixsignaalinformatie.xml.gz en zet het in `msi-parser/files/Matrixsignaalinformatie.xml.gz`.
* Voer binnen Eclipse de klasse `MsiExporter` uit als Java-applicatie.
* Installeer PostgreSQL 10 en PostGIS 2.5 indien nodig.
* Maak een nieuwe database `ndssmapmatching` (of hergebruik hem als deze database al bestaat).
* Voer de queries van `msi-parser/queries/create.sql` uit in de nieuwe database.
* Voer de queries van `msi-parser/files/msi_displays.sql` uit in de nieuwe database.

## inrix-parser

De INRIX parser kan gebruikt worden om het INRIX FCD netwerk in OpenLR formaat te parsen. Voorbeeld van gebruik:

* Installeer Eclipse.
* Importeer de map `inrix-parser` als Maven project in Eclipse.
* Download een *.xml bestanden met het OpenLR netwerk van INRIX en zet het in `inrix-parser/files/`.
* Voer binnen Eclipse de klasse `InrixExporter` uit als Java-applicatie.
* Installeer PostgreSQL 10 en PostGIS 2.5 indien nodig.
* Maak een nieuwe database `ndssmapmatching` (of hergebruik hem als deze database al bestaat).
* Voer de queries van `inrix-parser/queries/create.sql` uit in de nieuwe database.
* Voer de queries van `inrix-parser/files/inrix_nodes.sql` en `inrix-parser/files/inrix_segments.sql` uit in de nieuwe database. Vanwege de grootte kan het best het commando als `psql -f inrix_nodes.sql -p 5432 -d ndssmapmatching -U postgres` gebruikt worden (`psql` wordt meegeleverd met PostgreSQL).

## NDW basisnetwerk importeren

Het NDW basisnetwerk is het netwerk waar de meeste data op gemapt moet worden. We hebben dit aangeleverd gekregen als PostgreSQL backup bestand: `basemap_190101`.

* Installeer PostgreSQL 10 en PostGIS 2.5 indien nodig.
* Maak een nieuwe database `ndssmapmatching` (of hergebruik hem als deze database al bestaat).
* Voer in de nieuwe database de volgende query uit: `CREATE SCHEMA IF NOT EXISTS basemaps; CREATE EXTENSION IF NOT EXISTS postgis;`.
* Restore het bestand `basemap_190101` in de nieuwe database.

## NWB netwerk importeren

Het Nationaal Wegenbestand (NWB) bevat gegevens die gebruikt kunnen worden om het basisnetwerk van het NDW te verrijken. Een beschrijving is te vinden op https://www.rijkswaterstaat.nl/zakelijk/zakendoen-met-rijkswaterstaat/werkwijzen/werkwijze-in-gww/data-eisen-rijkswaterstaatcontracten/nationaal-wegenbestand.aspx

* Installeer PostgreSQL 10 en PostGIS 2.5 indien nodig.
* Maak een nieuwe database `ndssmapmatching` (of hergebruik hem als deze database al bestaat).
* Download een versie van het NWB dat wat datum betreft in de buurt zit van het basisnetwerk van het NDW, bijvoorbeeld: https://www.rijkswaterstaat.nl/apps/geoservices/geodata/dmc/nwb-wegen/geogegevens/shapefile/Nederland_totaal/01-01-2019.zip
* Pak het bestand uit.
* Voer het commando `shp2pgsql.exe -s 28992 -S -t 2D -I Wegvakken\Wegvakken.shp nwb > nwb.sql` uit in de uitgepakte map (`shp2pgsql` wordt meegeleverd met PostgreSQL).
* Voer de queries van `nwb.sql` uit in de nieuwe database. Vanwege de grootte kan het best een commando als `psql -f nwb.sql -p 5432 ndssmapmatching postgres > out.txt` gebruikt worden (`psql` wordt meegeleverd met PostgreSQL).

## MST shapefiles importeren 

* Installeer PostgreSQL 10 en PostGIS 2.5 indien nodig.
* Maak een nieuwe database `ndssmapmatching` (of hergebruik hem als deze database al bestaat).
* Download een versie van de MST shapefiles van: http://opendata.ndw.nu/   Bijvoorbeeld: 046.2_Levering_NDW_Shapefiles_20190306.zip
* Pak het bestand uit.
* Voer het commando `shp2pgsql.exe -s 4326 -S -t 2D -I Meetvakken.shp measurement_site_lines_shapefile > mst_meetvakken.sql` uit in de uitgepakte map (`shp2pgsql` wordt meegeleverd met PostgreSQL).
* Voer de queries van `mst_meetvakken.sql` uit in de nieuwe database. Vanwege de grootte kan het best een commando als `psql -f mst_meetvakken.sql -p 5432 ndssmapmatching postgres > out.txt` gebruikt worden (`psql` wordt meegeleverd met PostgreSQL).
* Voer het commando `shp2pgsql.exe -s 28992:4326 -S -t 2D -I Telpunten.shp measurement_site_points_shapefile > mst_telpunten.sql` uit in de uitgepakte map (`shp2pgsql` wordt meegeleverd met PostgreSQL).
* Voer de queries van `mst_telpunten.sql` uit in de nieuwe database. Vanwege de grootte kan het best een commando als `psql -f mst_telpunten.sql -p 5432 ndssmapmatching postgres > out.txt` gebruikt worden (`psql` wordt meegeleverd met PostgreSQL).

## routing-map-matcher

De routing map matcher gebruikt routezoekalgoritmes om bronnen te mappen op het basisnetwerk.

* Installeer Eclipse.
* Importeer de map `routing-map-matcher` als Maven project in Eclipse.
* Installeer PostgreSQL 10 en PostGIS 2.5 indien nodig.
* Maak een nieuwe database `ndssmapmatching` (of hergebruik hem als deze database al bestaat).
* Voer de stappen uit onder het kopje "NDW basisnetwerk importeren".
* Voer de stappen uit onder het kopje "NWB netwerk importeren".
* Voer de stappen uit onder het kopje "MST shapefiles importeren".
* Voer de stappen uit onder het kopje "mst-parser".
* Voer de stappen uit onder het kopje "waze-parser".
* Voer binnen Eclipse de klasse `RoutingMapMatcher` uit als Java-applicatie. De applicatie sluit af met de foutmelding dat `POSTGRES_END_POINT` niet gedefinieerd is.
* Open de run configuration van `RoutingMapMatcher` en ga naar het tabblad "Environment". Voeg de volgende environment variables toe:
    * `POSTGRES_END_POINT`: `localhost:5432` (of pas aan indien nodig)
    * `POSTGRES_DATABASE`: `ndssmapmatching`
    * `POSTGRES_USERNAME`: `postgres` (of pas aan indien nodig)
    * `POSTGRES_PASSWORD`: `postgres` (of pas aan indien nodig)
* Voer de aangepaste run configuration uit.
* De applicatie voert nu de volgende stappen uit:
    * Voor de MST data op basis van `measurement.xml.gz`:
        * Het NDW basisnetwerk wordt ingeladen exclusief de wegvakken waarvan het attribuut clazz gelijk is aan 41, 42 of 43.
        * Alle niet-FCD locaties in de MST met een start point en een end point worden ingeladen en gemapt op het basisnetwerk.
        * Het resultaat van de mapping wordt weggeschreven (of overschreven) in de nieuwe tabel `public.measurement_site_line_matches`.
    * Voor het NWB netwerk:
        * Het volledige NDW basisnetwerk wordt ingeladen.
        * Alle NWB links worden worden ingeladen en gemapt op het basisnetwerk. Aangezien het hier om meerdere miljoenen links gaat kan dit een aantal uren duren.
        * Het resultaat van de mapping wordt weggeschreven (of overschreven) in de nieuwe tabel `public.nwb_matches`.
    * Voor de waze jams:
        * Het volledige NDW basisnetwerk wordt ingeladen.
        * Alle waze jams worden ingeladen en gemapt op het basisnetwerk.
        * Het resultaat van de mapping wordt weggeschreven (of overschreven) in de nieuwe tabel `public.waze_jams_matches`.
    * Voor de waze irregularities:
        * Het volledige NDW basisnetwerk wordt ingeladen.
        * Alle waze irregularities worden ingeladen en gemapt op het basisnetwerk.
        * Het resultaat van de mapping wordt weggeschreven (of overschreven) in de nieuwe tabel `public.waze_irregularities_matches`.
    * Voor de MST data op basis van de shapefile:
        * Het volledige NDW basisnetwerk wordt ingeladen.
        * Alle lijnlocaties uit de shapefile "Meetvakken" worden ingeladen en gemapt op het basisnetwerk.
        * Het resultaat van de mapping wordt weggeschreven (of overschreven) in de nieuwe tabel `public.measurement_site_lines_shapefile_matches`.
* Instellen van parameters voor het start-to-end algoritme kan met de constantes in `StartToEndMapMatcher.java`.
* Instellen van parameters voor het Viterbi algoritme kan met de constantes in `ViterbiLineStringMapMatcher.java`.

## NDW basisnetwerk verrijken met NWB attributen

Het is mogelijk om een mapping van het NWB netwerk op het NDW basisnetwerk te gebruiken om attributen over te zetten van het NWB naar het NDW basisnetwerk.

* Voer de stappen uit onder het kopje "routing-map-matcher".
* Voer in de database `ndssmapmatching` de query van het bestand `sql/basemaps_segments_190101_nwb_attributes.sql` uit om de mapping van de "routing-map-matcher" te gebruiken om attributen over te zetten van het NWB naar het NDW basisnetwerk:
    * Alleen mappings met een pad en betrouwbaarheid >= 80 worden gebruik
    * Per NDW-wegvak:
        * Zoek alle mappings waar het NDW-wegvak voor minimaal 20% in de route zit
        * Sorteer de mappings op score: betrouwbaarheid * fractie dat wegvak in de route zit
        * Hevel attributen over van de mapping met de beste score
* De gekoppelde attributen staan in de (mogelijk nieuw aangemaakte) tabel `basemaps.segments_190101_nwb_attributes` met status `match`.

## NDW basisnetwerk verder verrijken met interpoleren van NWB attributen

Het is mogelijk om gaten in de verrijkte NWB attributen van het NDW basisnetwerk op te vullen.

* Voer de stappen uit onder het kopje "NDW basisnetwerk verrijken met NWB attributen".
* Voer in de database `ndssmapmatching` de query van het bestand `sql/basemaps_segments_190101_nwb_attributes_interpolated.sql` uit (vanwege de hoeveelheid routes die moet worden gevonden kan vele uren duren):
    * Per eiland van wegvakken zonder NWB attributen:
        * Tussen alle aangrenzende wegvakken met dezelfde NWB attributen:
            * Zoek de korste route door het eiland
        * Voor ieder wegvak dat precies in één kortste route zit:
            * Gebruik de bijbehorende NWB attributen voor dit wegvak.
* De gekoppelde attributen staan in de tabel `basemaps.segments_190101_nwb_attributes` met status `interpolated`.

## inrix-map-matcher

De INRIX map matcher gebruikt de OpenLR decoder van TomTom om INRIX segmenten te mappen op het basisnetwerk.

* Installeer Eclipse.
* Importeer de map `inrix-map-matcher` als Maven project in Eclipse.
* Installeer PostgreSQL 10 en PostGIS 2.5 indien nodig.
* Maak een nieuwe database `ndssmapmatching` (of hergebruik hem als deze database al bestaat).
* Voer de stappen uit onder het kopje "NDW basisnetwerk importeren".
* Voer de stappen uit onder het kopje "inrix-parser".
* Voer de queries in `function_check_offsets_full_matches.sql`, `function_compute_linestring_of_match.sql` en `function_compute_linestring_and_reliability_of_match.sql` (in de map `inrix-map-matcher/queries/`) uit in de nieuwe database.
* Voer binnen Eclipse de klasse `InrixMapMatcher` uit als Java-applicatie. De applicatie sluit af met de foutmelding dat `POSTGRES_END_POINT` niet gedefinieerd is.
* Open de run configuration van `InrixMapMatcher` en ga naar het tabblad "Environment". Voeg de volgende environment variables toe:
    * `POSTGRES_END_POINT`: `localhost:5432` (of pas aan indien nodig)
    * `POSTGRES_DATABASE`: `ndssmapmatching`
    * `POSTGRES_USERNAME`: `postgres` (of pas aan indien nodig)
    * `POSTGRES_PASSWORD`: `postgres` (of pas aan indien nodig)
* Voer de aangepaste run configuration uit.
* De applicatie voert nu de volgende stappen uit:
    * Het volledige NDW basisnetwerk wordt ingeladen.
    * Alle INRIX segmenten worden worden ingeladen en gemapt op het basisnetwerk. Dit kan een aantal uren duren.
    * Het resultaat van de mapping wordt weggeschreven (of overschreven) in de nieuwe tabel `public.inrix_segment_matches`.
* Instellen van parameters voor de OpenLR decoder kan in `inrix-map-matcher/src/main/resources/OpenLR_Decoder_properties.xml`

## Map matching van MST punten

De map matching MST punten wordt uitgevoerd in PostgreSQL.

* Voer de stappen uit onder het kopje "NDW basisnetwerk verrijken met NWB attributen".
* Voer de stappen uit onder het kopje "MST shapefiles importeren".
* Voer in de database `ndssmapmatching` de query van het bestand `sql/measurement_site_point_with_bearing_matches.sql` uit om de MST punten uit de shapefile te map matchen naar het NDW basisnetwerk
* De map matching staat in de (mogelijk nieuw aangemaakte) tabel `measurement_site_points_shapefile_matches`
* Voer in de database `ndssmapmatching` de query van het bestand `sql/measurement_site_point_matches.sql` uit om de overige MST punten te map matchen naar het NDW basisnetwerk
* De map matching staat in de (mogelijk nieuw aangemaakte) tabel `measurement_site_point_matches`.
