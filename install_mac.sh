git checkout main
git pull
mvn clean package
cd target
chmod a+x beat-*.jar
export name=`ls beat-*.jar`
cp *.jar ..
cd ..
sed -i '' "s/beat-1.0-SNAPSHOT.jar/${name}/g" net.vince.beat-bot.plist
cp net.vince.beat-bot.plist ~/Library/LaunchAgents/net.vince.beat-bot.plist
cp net.vince.beat-bot.po-token-generator.plist ~/Library/LaunchAgents/net.vince.beat-bot.po-token-generator.plist
launchctl unload ~/Library/LaunchAgents/net.vince.beat-bot.plist
launchctl unload ~/Library/LaunchAgents/net.vince.beat-bot.po-token-generator.plist
launchctl load ~/Library/LaunchAgents/net.vince.beat-bot.plist
launchctl load ~/Library/LaunchAgents/net.vince.beat-bot.po-token-generator.plist