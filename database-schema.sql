


-- this procedure summarizes likes and dislikes for a level
-- it should be run after every insert or update on the likes table
delimiter //
CREATE PROCEDURE mcmaker.level_summary (IN level_id_param BINARY(16))
BEGIN

  DECLARE likes, dislikes INT DEFAULT 0;
  -- calculate likes
  SELECT
    SUM(CASE WHEN `level_likes`.`dislike` = 0 THEN 1 ELSE 0 END) into likes
  FROM
    mcmaker.level_likes
  WHERE
    level_likes.level_id = level_id_param
  GROUP BY
    level_likes.level_id;
  -- calculate dislikes
  SELECT
    SUM(CASE WHEN `level_likes`.`dislike` = 1 THEN 1 ELSE 0 END) into dislikes 
  FROM
    mcmaker.level_likes
  WHERE
    level_likes.level_id = level_id_param
  GROUP BY
    level_likes.level_id;
  -- update summaries on the master table
  UPDATE
    mcmaker.levels
  SET
    level_likes = likes,
    level_dislikes = dislikes
  WHERE
    level_id = level_id_param;
END;//
delimiter ;



-- this trigger summarizes level likes and dislikes after every update
delimiter //
CREATE TRIGGER mcmaker.after_like_update AFTER UPDATE ON mcmaker.level_likes
FOR EACH ROW
BEGIN
  CALL mcmaker.level_summary(NEW.level_id);
END;//
delimiter ;



-- this trigger summarizes level likes and dislikes after every insert
delimiter //
CREATE TRIGGER mcmaker.after_like_insert AFTER INSERT ON mcmaker.level_likes
FOR EACH ROW
BEGIN
  CALL mcmaker.level_summary(NEW.level_id);
END;//
delimiter ;



-- this trigger summarizes level likes and dislikes after every delete
delimiter //
CREATE TRIGGER mcmaker.after_like_delete AFTER DELETE ON mcmaker.level_likes
FOR EACH ROW
BEGIN
  CALL mcmaker.level_summary(OLD.level_id);
END;//
delimiter ;



-- this procedure removes old schematic backups from the specified leaving only the two more recent ones along with the current (latest)
DROP PROCEDURE mcmaker.delete_old_schematic_backups_by_level_id;
delimiter //
CREATE PROCEDURE mcmaker.delete_old_schematic_backups_by_level_id (IN level_id_param BINARY(16))
BEGIN

  DECLARE total_count, delete_count INT DEFAULT 0;

  SELECT
    COUNT(1) into total_count
  FROM
    mcmaker.schematics
  WHERE
    schematics.level_id = level_id_param;

  IF total_count > 3 THEN
    SET delete_count = total_count - 3;
  END IF;

  IF delete_count > 0 THEN
    DELETE FROM mcmaker.schematics where level_id = level_id_param ORDER BY updated ASC LIMIT delete_count;
  END IF;

END;//
delimiter ;



-- this procedure goes through all existing level ids calling the procedure above for each one (mcmaker.delete_schematic_backups)
DROP PROCEDURE mcmaker.delete_old_schematic_backups;
delimiter //
CREATE PROCEDURE mcmaker.delete_old_schematic_backups ()
BEGIN

   DECLARE v_finished INTEGER DEFAULT 0;
   DECLARE v_level_id BINARY(16) DEFAULT 0;

   DECLARE level_cursor CURSOR FOR 
   SELECT level_id FROM mcmaker.levels;

   DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_finished = 1;

   OPEN level_cursor;

   get_level: LOOP

     FETCH level_cursor INTO v_level_id;

     IF v_finished = 1 THEN
       LEAVE get_level;
     END IF;

     CALL mcmaker.delete_old_schematic_backups_by_level_id(v_level_id);

   END LOOP get_level;

   CLOSE level_cursor;

END;//
delimiter ;



-- this trigger removes old backups (if they exist) when a level is updated
DROP TRIGGER mcmaker.delete_old_schematic_backups_after_update;
delimiter //
CREATE TRIGGER mcmaker.delete_old_schematic_backups_after_update AFTER UPDATE ON mcmaker.levels
FOR EACH ROW
BEGIN
  CALL mcmaker.delete_old_schematic_backups_by_level_id(NEW.level_id);
END;//
delimiter ;



