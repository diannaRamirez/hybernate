package org.hibernate.userguide.mapping.basic;

import java.util.BitSet;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-custom-type-BitSetType-example[]
public class BitSetType
        extends AbstractSingleColumnStandardBasicType<BitSet> {

    public static final BitSetType INSTANCE = new BitSetType();

    public BitSetType() {
        super( VarcharJdbcTypeDescriptor.INSTANCE, BitSetJavaType.INSTANCE );
    }

    @Override
    public String getName() {
        return "bitset";
    }

}
//end::basic-custom-type-BitSetType-example[]
