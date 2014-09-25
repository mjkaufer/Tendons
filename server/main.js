var fs = require("fs");
var file = "data/users.db";
var exists = fs.existsSync(file);

var express = require('express');
var api = express();

var sqlite3 = require("sqlite3").verbose();
var db = new sqlite3.Database(file);
var estimoteUUID = "";


db.serialize(function(){
	// db.run("CREATE TABLE IF NOT EXISTS users (user VARCHAR(128),status INT(12),RID int(11) NOT NULL auto_increment,primary KEY (RID));");//makes our table
	db.run("CREATE TABLE IF NOT EXISTS users (user VARCHAR(128),status INT(12));");//makes our table
	db.run("DELETE FROM users WHERE status = -1;");
	db.run("INSERT INTO users (status) VALUES (-1);");
});

var upsert = db.prepare("INSERT INTO users "
	+ "(user, status) "
	+ "SELECT ?, 1 "
	+ "FROM users "
	+ "WHERE NOT EXISTS (SELECT * "
	+ "FROM users "
	+ "WHERE user = ?);");//inserts user with perm 1


// api.post('/data/:user/:method/:estimoteUUID', function(req, res){
api.post('/data/:user/:method', function(req, res){

	// if(req.params.estimoteUUID != estimoteUUID)
	// 	return;to be implemented later to verify that they're at the estimote, even though it's not the best verif

	if(req.params.method.toLowerCase() == "locationchange")//mod status from 1 to 0
	    db.run("UPDATE users SET status = (status + 1) % 2 WHERE user = " + req.params.user.toLowerCase(), function(err, row){//switches from 1 to 0 - 1 is in so default will be 1
	        if (err){
	            console.err(err);
	            res.status(500);
	        }
	        else {
	            res.status(202);
	        }
	        res.end();
	    });
	else if(req.params.method.toLowerCase() == "create"){
		runUpsert(req.params.user.toLowerCase());
		res.status(200);
		res.end("Added user");
	}
	else{
		res.status(404);
		res.end("Not found");
	}

});

api.get('/', function(req, res){
	res.status(200);
	res.end("Nothing here.");
})


api.get('/data/users', function(req, res){
	var ret = "{users:[";

	db.each("SELECT * FROM users", function(err, row) {
		if(row.status != -1){//apparently the upsert code only works if there's something already in the DB, so we'll have a thing with a val of -1
			console.log("{user:" + row.user + ", status:" + row.status + "}");
			ret+="{user:'" + row.user + "', status:" + row.status + "}";

			ret += "]}";

			res.status(200);
			res.end(ret);
		}
		
	});

});

api.listen(3000);

console.log("listening on http://localhost:3000");

function runUpsert(user){

	upsert.run(user);

}
