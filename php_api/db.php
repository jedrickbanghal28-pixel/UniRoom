<?php
header('Content-Type: application/json; charset=UTF-8');
$config=require __DIR__.'/config.php';
try{$pdo=new PDO("pgsql:host={$config['host']};port={$config['port']};dbname={$config['dbname']}",$config['username'],$config['password'],[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);}
catch(PDOException $e){http_response_code(500);echo json_encode(['success'=>false,'message'=>'Database connection failed.']);exit;}
function body(){return json_decode(file_get_contents('php://input'),true) ?: [];}
?>
