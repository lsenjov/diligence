-- :name select-persons :? :*
-- :doc searches for banned persons by first and last name
SELECT * FROM banned_person
WHERE (BD_PER_NAME LIKE :first-name)
AND (BD_PER_NAME LIKE :last-name)
