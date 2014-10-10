var fs = require("fs");
var file = "data/users.db";//if you're going to alter this, make sure a path exists
var exists = fs.existsSync(file);

var express = require('express');
var api = express();

var sqlite3 = require("sqlite3").verbose();
var db = new sqlite3.Database(file);
var estimoteUUID = "";

db.serialize(function(){
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

/*
	DETAILS
	1 MEANS OUTSIDE
	0 MEANS INSIDE
*/


var toggle = db.prepare("UPDATE users SET status = (status + 1) % 2 WHERE user = ?");
var enter = db.prepare("UPDATE users SET status = 0 WHERE user = ?");
var exit = db.prepare("UPDATE users SET status = 1 WHERE user = ?");


// api.post('/data/:user/:method/:estimoteUUID', function(req, res){
api.post('/data/:method/:user', function(req, res){//methods - enter, leave, create

	// if(req.params.estimoteUUID != estimoteUUID)
	// 	return;to be implemented later to verify that they're at the estimote, even though it's not the best verif

	if(req.params.method.toLowerCase() == "locationchange")//mod status from 1 to 0
	    toggle.run(req.params.user.toLowerCase(), function(e, row){//switches from 1 to 0 - 1 is out so default will be 1
			res.status(202);//err keeps throwing errs
	        
	        res.end("Updated - toggled");
	    });
	else if(req.params.method.toLowerCase() == "create"){
		runUpsert(req.params.user.toLowerCase());
		res.status(200);
		res.end("Added user " + req.params.user.toLowerCase());
	}
	else if(req.params.method.toLowerCase() == "enter"){//set status to 0, as the user is now inside
		enter.run(req.params.user.toLowerCase(), function(e,row){
			res.status(202);
			res.end("Updated - inside");
		})
	}
	else if(req.params.method.toLowerCase() == "leave"){//set status to 1, as the user is now outside
		exit.run(req.params.user.toLowerCase(), function(e,row){
			res.status(202);
			res.end("Updated - outside");
		})
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
	console.log("THINGS");
	db.all("SELECT * FROM users;", function(err, all) {

		console.log(all);
		
		ret = all.filter(function(e){
			return e.status != -1;//only return those without a status of -1
		});
	
		res.status(200);
		res.end(JSON.stringify(ret));	
	});


	
});

api.listen(8080);

console.log("listening on http://localhost:8080");

function runUpsert(user){

	upsert.run(user);

}
