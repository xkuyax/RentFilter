The plan for this project is to help me find a good apartment for me. I will be looking for one in December 
with a start date of end of February. As its not quite december yet, i would like to test/develop the application before hand.
I am going to have requirements, which will be in REQUIREMENTS.md. The app should filter the following data sources:
- https://www.willhaben.at/iad/immobilien
- https://genossenschaften.immo/
- https://www.grawewohnen.at/

I might add another options like immobilienscout24/derstandard, so it needs to be generic to handle all of those.
I would like to create this with spring boot, as this is what im most comfortable with.
Database can be a local sql file, or a docker instance with postgres. I plan on running this on my vps, so cron job for fetching
new information is feasible. 

In addition to the pure backend I previously really enjoyed viewing the map of my local area in the browser and seeing dots with the locations and being able to click on them/view details.
