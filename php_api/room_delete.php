<?php
require 'db.php';
$d=body();$id=(int)($d['id']??0);if($id<=0){http_response_code(400);echo json_encode(['success'=>false,'message'=>'Room ID is required.']);exit;}
try{$s=$pdo->prepare('DELETE FROM rooms WHERE id=:i');$s->execute([':i'=>$id]);if($s->rowCount()===0){http_response_code(404);echo json_encode(['success'=>false,'message'=>'Room not found.']);exit;}echo json_encode(['success'=>true,'message'=>'Room deleted successfully.']);}catch(PDOException $e){if($e->getCode()==='23503'){http_response_code(409);echo json_encode(['success'=>false,'message'=>'This room is used by a schedule and cannot be deleted.']);}else{http_response_code(500);echo json_encode(['success'=>false,'message'=>'Delete operation failed.']);}}
?>
