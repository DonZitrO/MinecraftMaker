


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
