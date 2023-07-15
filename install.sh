git checkout main
git pull
mvn clean package
cd target
chmod a+x beat-*.jar
export name=`ls beat-*.jar`
cd ..
sed -i "s/beat-1.0-SNAPSHOT.jar/${name}/g" beat-bot.service
sudo cp beat-bot.service /lib/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable beat-bot.service
sudo systemctl restart beat-bot.service
