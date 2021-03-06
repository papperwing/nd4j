package org.nd4j.autodiff.functions.impl.unary.transform.shape;

import org.nd4j.autodiff.ArrayField;
import org.nd4j.autodiff.functions.AbstractUnaryFunction;
import org.nd4j.autodiff.functions.DifferentialFunction;
import org.nd4j.autodiff.opstate.OpState;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.linalg.util.ArrayUtil;

import java.util.Collections;
import java.util.List;

public class Transpose extends AbstractUnaryFunction<ArrayField> {
    public Transpose(SameDiff sameDiff, DifferentialFunction<ArrayField> i_v, Object[] extraArgs) {
        super(sameDiff,i_v, ArrayUtil.reverseCopy(i_v.getValue(true).getInput().getShape()), OpState.OpType.SHAPE,extraArgs);
    }

    @Override
    public ArrayField doGetValue() {
        return sameDiff.getArrayFactory().transpose(arg().getValue(true));
    }

    @Override
    public List<DifferentialFunction<ArrayField>> diff(List<DifferentialFunction<ArrayField>> i_v) {
        arg().setGradient(this);
        return Collections.singletonList(this);
    }



    @Override
    public String functionName() {
        return "transpose";
    }
}
