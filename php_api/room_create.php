<?php
require 'db.php';
$d=body();
foreach(['room_name','building','floor','capacity','status'] as $f) if(!isset($d[$f])||trim((string)$d[$f])===''){http_response_code(400);echo json_encode(['success'=>false,'message'=>ucwords(str_replace('_',' ',$f)).' is required.']);exit;}
$floor=(int)$d['floor'];$capacity=(int)$d['capacity'];$status=trim($d['status']);
if($floor<0||$capacity<=0){http_response_code(400);echo json_encode(['success'=>false,'message'=>'Floor and capacity must be valid numbers.']);exit;}
if(!in_array($status,['Available','Occupied','Reserved','Maintenance'],true)){http_response_code(400);echo json_encode(['success'=>false,'message'=>'Invalid room status.']);exit;}
try{$s=$pdo->prepare('INSERT INTO rooms(room_name,building,floor,capacity,status) VALUES(:n,:b,:f,:c,:s) RETURNING id');$s->execute([':n'=>trim($d['room_name']),':b'=>trim($d['building']),':f'=>$floor,':c'=>$capacity,':s'=>$status]);$id=$s->fetchColumn();$q=$pdo->prepare('SELECT id,room_name,building,floor,capacity,status FROM rooms WHERE id=:i');$q->execute([':i'=>$id]);echo json_encode(['success'=>true,'message'=>'Room added successfully.','room'=>$q->fetch(PDO::FETCH_ASSOC)]);}catch(PDOException $e){if($e->getCode()==='23505'){http_response_code(409);echo json_encode(['success'=>false,'message'=>'Room name already exists.']);}else{http_response_code(500);echo json_encode(['success'=>false,'message'=>'Create operation failed.']);}}
?>
