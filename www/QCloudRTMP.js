var exec = require('cordova/exec');

var Play = function() {};
Play.play = function(play_type, play_param, success, error) {
    exec(success, error, "QCloudRTMP", "play", [play_type,play_param]);
};

module.exports = Play;