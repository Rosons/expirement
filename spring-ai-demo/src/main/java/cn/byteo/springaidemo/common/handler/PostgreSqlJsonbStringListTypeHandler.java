package cn.byteo.springaidemo.common.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL jsonb 数组字段与 {@code List<String>} 的双向转换（如 {@code ["a","b"]}）。
 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PostgreSqlJsonbStringListTypeHandler extends BaseTypeHandler<List<String>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        jsonObject.setValue(writeJson(parameter));
        ps.setObject(i, jsonObject);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return readJson(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return readJson(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return readJson(cs.getString(columnIndex));
    }

    private String writeJson(List<String> parameter) throws SQLException {
        try {
            return OBJECT_MAPPER.writeValueAsString(parameter == null ? new ArrayList<>() : parameter);
        } catch (JsonProcessingException ex) {
            throw new SQLException("jsonb 数组字段序列化失败", ex);
        }
    }

    private List<String> readJson(String json) throws SQLException {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(json, LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new SQLException("jsonb 数组字段反序列化失败", ex);
        }
    }
}
