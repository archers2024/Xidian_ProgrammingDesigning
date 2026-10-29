package com.campus.events.common.mybatis;

import com.campus.events.common.TimeSupport;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;

@MappedTypes(OffsetDateTime.class)
@MappedJdbcTypes(JdbcType.TIMESTAMP)
public class OffsetDateTimeTypeHandler extends BaseTypeHandler<OffsetDateTime> {

    @Override
    public void setNonNullParameter(
            PreparedStatement ps, int index, OffsetDateTime parameter, JdbcType jdbcType) throws SQLException {
        ps.setTimestamp(index, Timestamp.from(parameter.toInstant()));
    }

    @Override
    public OffsetDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return read(rs.getTimestamp(columnName));
    }

    @Override
    public OffsetDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return read(rs.getTimestamp(columnIndex));
    }

    @Override
    public OffsetDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return read(cs.getTimestamp(columnIndex));
    }

    private static OffsetDateTime read(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant().atZone(TimeSupport.ZONE).toOffsetDateTime();
    }
}
