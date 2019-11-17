package db

import slick.jdbc.PostgresProfile

trait DbProfile extends PostgresProfile

object DbProfile extends DbProfile