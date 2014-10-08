# SqlFace

_SqlFace_ stands for "SQL on interface" and it's a Java annotation processor
for generating boilerplate JDBC code from annotated interfaces like this:

    @SQL
    public interface BooksDao {
        @Query("select * from books")
        List<Book> getAll(RowMapper<Book> rowMapper);

        @Query("select * from books where id = ?")
        Book getById(int id, RowMapper<Book> rowMapper);

        @Update("insert into books (id, name, author) values (?, ?, ?)")
        void create(int id, String name, String author);

        @Update("delete from books where id = ?")
        void delete(int id);
    }

Note that this is __not__ meant to be used, it's more of a demo. For serious
usage, at least transaction handling would have to be added.

## Preparation

Make sure you use the Sun/Oracle JDK, at least version 7.

Maven 3 is required, too.

## Usage

`mvn clean install`

Then see `example/target/generated-sources/annotations/sqlface`.
