package com.github.housepower.jdbc.data.type.complex;

import com.github.housepower.jdbc.connect.PhysicalInfo;
import com.github.housepower.jdbc.data.IDataType;
import com.github.housepower.jdbc.misc.SQLLexer;
import com.github.housepower.jdbc.misc.StringView;
import com.github.housepower.jdbc.misc.Validate;
import com.github.housepower.jdbc.serializer.BinaryDeserializer;
import com.github.housepower.jdbc.serializer.BinarySerializer;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DataTypeEnum8 implements IDataType {

    private String name;
    private Byte[] values;
    private String[] names;

    public DataTypeEnum8(String name, String[] names, Byte[] values) {
        this.name = name;
        this.names = names;
        this.values = values;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int sqlTypeId() {
        return Types.VARCHAR;
    }

    @Override
    public Object defaultValue() {
        return values[0];
    }

    @Override
    public Class javaTypeClass() {
        return String.class;
    }

    @Override
    public Object deserializeTextQuoted(SQLLexer lexer) throws SQLException {
        return lexer.stringLiteral();
    }

    @Override
    public void serializeBinary(Object data, BinarySerializer serializer) throws SQLException, IOException {
        Validate.isTrue(data instanceof String || data instanceof StringView,
            "Expected Enum8 Parameter, but was " + data.getClass().getSimpleName());

        for (int i = 0; i < names.length; i++) {
            if (data.equals(names[i])) {
                serializer.writeByte(values[i]);
                return;
            }
        }

        String message = "Expected ";
        for (int i = 0; i < names.length; i++) {
            if (i > 0)
                message += " OR ";
            message += names[i];
        }

        throw new SQLException(message + " , but was " + String.valueOf(data));
    }

    @Override
    public Object deserializeBinary(BinaryDeserializer deserializer) throws SQLException, IOException {
        byte value = deserializer.readByte();
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return names[i];
            }
        }
        throw new SQLException("");
    }

    @Override
    public void serializeBinaryBulk(Object[] data, BinarySerializer serializer) throws SQLException, IOException {
        for (Object datum : data) {
            serializeBinary(datum, serializer);
        }
    }

    @Override
    public Object[] deserializeBinaryBulk(int rows, BinaryDeserializer deserializer) throws SQLException, IOException {
        String[] data = new String[rows];
        for (int row = 0; row < rows; row++) {
            data[row] = (String) deserializeBinary(deserializer);
        }
        return data;
    }

    public static IDataType createEnum8Type(SQLLexer lexer, PhysicalInfo.ServerInfo serverInfo) throws SQLException {
        Validate.isTrue(lexer.character() == '(');
        List<Byte> enumValues = new ArrayList<Byte>();
        List<String> enumNames = new ArrayList<String>();

        for (int i = 0; i < 256; i++) {
            enumNames.add(String.valueOf(lexer.stringLiteral()));
            Validate.isTrue(lexer.character() == '=');
            enumValues.add(lexer.numberLiteral().byteValue());

            char character = lexer.character();
            Validate.isTrue(character == ',' || character == ')');

            if (character == ')') {
                StringBuilder builder = new StringBuilder("Enum8(");
                for (int index = 0; index < enumNames.size(); index++) {
                    if (index > 0)
                        builder.append(",");
                    builder.append("'").append(enumNames.get(index)).append("'")
                        .append(" = ").append(enumValues.get(index));
                }
                return new DataTypeEnum8(builder.append(")").toString(),
                    enumNames.toArray(new String[enumNames.size()]), enumValues.toArray(new Byte[enumValues.size()]));
            }
        }
        throw new SQLException("DataType Enum8 size must be less than 256");
    }
}
