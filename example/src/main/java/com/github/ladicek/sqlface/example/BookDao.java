package com.github.ladicek.sqlface.example;

import com.github.ladicek.sqlface.Query;
import com.github.ladicek.sqlface.RowMapper;
import com.github.ladicek.sqlface.SQL;
import com.github.ladicek.sqlface.Update;

import java.util.List;

@SQL
public interface BookDao {
    @Query("select * from books")
    List<Book> getAll(RowMapper<Book> rowMapper);

    @Query("select * from books where id = ?")
    Book getById(int id, RowMapper<Book> rowMapper);

    @Update("insert into books (id, name) values (?, ?)")
    void create(int id, String name);

    @Update("delete from books where id = ?")
    void delete(int id);
}
