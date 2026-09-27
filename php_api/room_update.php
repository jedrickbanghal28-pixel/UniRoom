<?php
require 'db.php';
$d=body();
foreach(['id','room_name','building','floor','capacity','status'] as $f) if(!isset($d[$f])||trim((string)$d[$f])===''){http_response_code(400);echo json_encode(['success'=>false,'message'=>ucwords(str_replace('_',' ',$f)).' is required.']);exit;}
$floor=(int)$d['floor'];$capacity=(int)$d['capacity'];$status=trim($d['status']);
if((int)$d['id']<=0||$floor<0||$capacity<=0){http_response_code(400);echo json_encode(['success'=>false,'message'=>'Invalid room data.']);exit;}
if(!in_array($status,['Available','Occupied','Reserved','Maintenance'],true)){http_response_code(400);echo json_encode(['success'=>false,'message'=>'Invalid room status.']);exit;}
try{$s=$pdo->prepare('UPDATE rooms SET room_name=:n,building=:b,floor=:f,capacity=:c,status=:s WHERE id=:i');$s->execute([':n'=>trim($d['room_name']),':b'=>trim($d['building']),':f'=>$floor,':c'=>$capacity,':s'=>$status,':i'=>(int)$d['id']]);if($s->rowCount()===0){http_response_code(404);echo json_encode(['success'=>false,'message'=>'Room not found.']);exit;}echo json_encode(['success'=>true,'message'=>'Room updated successfully.']);}catch(PDOException $e){if($e->getCode()==='23505'){http_response_code(409);echo json_encode(['success'=>false,'message'=>'Room name already exists.']);}else{http_response_code(500);echo json_encode(['success'=>false,'message'=>'Update operation failed.']);}}
?>
