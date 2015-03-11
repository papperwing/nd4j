package org.nd4j.linalg.api.ops.impl.scalar;

import org.apache.commons.math3.util.FastMath;
import org.nd4j.linalg.api.complex.IComplexNumber;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.BaseScalarOp;
import org.nd4j.linalg.api.ops.Op;

/**
 * Scalar max operation.
 * Returns the max of an element
 * in the ndarray of the specified number.
 * @author Adam Gibson
 */
public class ScalarMax extends BaseScalarOp {
    public ScalarMax(INDArray x, INDArray y, INDArray z, int n, Number num) {
        super(x, y, z, n, num);
    }

    public ScalarMax(INDArray x, Number num) {
        super(x, num);
    }

    public ScalarMax(INDArray x, INDArray y, INDArray z, int n, IComplexNumber num) {
        super(x, y, z, n, num);
    }

    public ScalarMax(INDArray x, IComplexNumber num) {
        super(x, num);
    }

    @Override
    public String name() {
        return "max_scalar";
    }

    @Override
    public IComplexNumber op(IComplexNumber origin, double other) {
        if(origin.absoluteValue().doubleValue() > complexNumber.absoluteValue().doubleValue())
            return origin;
        return complexNumber;
    }

    @Override
    public IComplexNumber op(IComplexNumber origin, float other) {
        if(origin.absoluteValue().doubleValue() > complexNumber.absoluteValue().doubleValue())
            return origin;
        return complexNumber;
    }

    @Override
    public IComplexNumber op(IComplexNumber origin, IComplexNumber other) {
        if(origin.absoluteValue().doubleValue() > complexNumber.absoluteValue().doubleValue())
            return origin;
        return complexNumber;
    }

    @Override
    public float op(float origin, float other) {
        return FastMath.max(origin, num.floatValue());
    }

    @Override
    public double op(double origin, double other) {
        return FastMath.max(origin,num.doubleValue());
    }

    @Override
    public double op(double origin) {
        return FastMath.max(origin,num.doubleValue());

    }

    @Override
    public float op(float origin) {
        return FastMath.max(origin, num.floatValue());

    }

    @Override
    public IComplexNumber op(IComplexNumber origin) {
        if(origin.absoluteValue().doubleValue() > complexNumber.absoluteValue().doubleValue())
            return origin;
        return complexNumber;
    }

    @Override
    public Op opForDimension(int index, int dimension) {
        if(num != null)
            return new ScalarMax(x.vectorAlongDimension(index,dimension),num);
        else
            return new ScalarMax(x.vectorAlongDimension(index, dimension),complexNumber);
    }

    @Override
    public void init(INDArray x, INDArray y, INDArray z, int n) {
        super.init(x, y, z, n);
        if(num != null)
            this.extraArgs = new Object[]{num};
        else
            this.extraArgs = new Object[]{complexNumber};

    }
}
