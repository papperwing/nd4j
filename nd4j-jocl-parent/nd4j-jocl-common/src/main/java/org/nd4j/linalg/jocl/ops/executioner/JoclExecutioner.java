/*
 * Copyright 2015 Skymind,Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.nd4j.linalg.jocl.ops.executioner;


import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.Accumulation;
import org.nd4j.linalg.api.ops.Op;
import org.nd4j.linalg.api.ops.ScalarOp;
import org.nd4j.linalg.api.ops.TransformOp;
import org.nd4j.linalg.api.ops.executioner.OpExecutioner;
import org.nd4j.linalg.jocl.SimpleOpencl;
import org.nd4j.linalg.jocl.buffer.OpenClDoubleDataBuffer;
import org.nd4j.linalg.jocl.buffer.OpenClFloatDataBuffer;
import org.nd4j.linalg.jocl.buffer.OpenclBuffer;
import org.nd4j.linalg.jocl.kernel.KernelFunctions;
import org.nd4j.linalg.jocl.util.PointerUtil;


/**
 * JCuda executioner.
 * <p/>
 * Runs ops directly on the gpu
 *
 * @author Adam Gibson
 */
public class JoclExecutioner implements OpExecutioner {
    private OpenclBuffer dummyFloatPointer, dummyDoublePointer;

    public JoclExecutioner() {
        SimpleOpencl.init();
        dummyFloatPointer = KernelFunctions.alloc(new float[]{1});
        dummyDoublePointer = KernelFunctions.alloc(new double[]{1});
    }

    @Override
    public Op exec(Op op) {
        if (op instanceof TransformOp) {
            TransformOp t = (TransformOp) op;
            invoke(t);
        } else if (op instanceof Accumulation) {
            Accumulation acc = (Accumulation) op;
            invoke(acc);
        } else if (op instanceof ScalarOp) {
            ScalarOp sc = (ScalarOp) op;
            invoke(sc);
        }
        return op;
    }


    private Pointer dummyDoublePointer() {
        return Pointer.to(dummyDoublePointer.pointer());
    }

    private Pointer dummyFloatPointer() {
        return Pointer.to(dummyFloatPointer.pointer());
    }

    @Override
    public INDArray execAndReturn(TransformOp op) {
        invoke(op);
        return op.z();
    }


    @Override
    public Accumulation execAndReturn(Accumulation op) {
        return (Accumulation) exec(op);
    }

    @Override
    public INDArray execAndReturn(ScalarOp op) {
        return exec(op).z();
    }


    @Override
    public Op exec(Op op, int dimension) {
        //only accumulate along a particular dimension
        if (op instanceof Accumulation) {
            Accumulation a = (Accumulation) op;
            return exec(a);
        }
        for (int i = 0; i < op.x().vectorsAlongDimension(dimension); i++) {
            Op op2 = op.opForDimension(i, dimension);
            exec(op2);
            if (op instanceof TransformOp) {
                TransformOp t = (TransformOp) op;
                TransformOp t2 = (TransformOp) op2;
                t.z().vectorAlongDimension(i, dimension).assign(t2.z());
            }


        }
        return op;
    }



    private Object[] convert(Object[] extraArgs,String dataType) {
        if(extraArgs == null)
            return null;
        Object[] ret = new Object[extraArgs.length];

        if(dataType.equals("double")) {
            double[] convert = PointerUtil.toDoubles(extraArgs);
            for(int i = 0; i < extraArgs.length; i++)
                ret[i] = convert[i];
        }
        else {
            float[] convert = PointerUtil.toFloats(extraArgs);
            for(int i = 0; i < extraArgs.length; i++)
                ret[i] = convert[i];

        }

        return ret;
    }



    @Override
    public INDArray execAndReturn(TransformOp op, int dimension) {
        for (int i = 0; i < op.x().vectorsAlongDimension(dimension); i++) {
            Op op2 = op.opForDimension(i, dimension);
            exec(op2);
            if (op instanceof TransformOp) {
                TransformOp t = op;
                TransformOp t2 = (TransformOp) op2;
                t.z().vectorAlongDimension(i, dimension).assign(t2.z());
            }


        }
        return op.z();
    }

    @Override
    public Accumulation execAndReturn(Accumulation op, int dimension) {
        return (Accumulation) exec(op, dimension);
    }

    @Override
    public INDArray execAndReturn(ScalarOp op, int dimension) {
        return exec(op, dimension).z();
    }


    private void invoke(Accumulation op) {
        OpenclBuffer xBuffer = (OpenclBuffer) op.x().data();
        Pointer xPointer = Pointer.to(xBuffer.buff()).withByteOffset(xBuffer.elementSize() * op.x().offset());
        OpenclBuffer result;
        int resultLength = 1000;
        if (op.x().data().dataType() == DataBuffer.DOUBLE) {
            double[] resultBuffer = new double[resultLength];
            for (int i = 0; i < resultBuffer.length; i++)
                resultBuffer[i] = op.zero().doubleValue();
            result = new OpenClDoubleDataBuffer(resultBuffer);


        } else {
            float[] resultBuffer = new float[resultLength];
            for (int i = 0; i < resultBuffer.length; i++)
                resultBuffer[i] = op.zero().floatValue();
            result = new OpenClFloatDataBuffer(resultBuffer);
        }

        if (op.y() != null) {
            OpenclBuffer yBuffer = (OpenclBuffer) op.y().data();
            Pointer yPointer = Pointer.to(yBuffer.buff()).withByteOffset(op.y().offset() * yBuffer.elementSize());

            //int n,int xOffset,int yOffset, double *dx, double *dy,int incx,int incy,double *result


            Object[] kernelParams = new Object[] {
                    new int[]{op.n()},
                    new int[]{op.x().offset()},
                    new int[]{op.y().offset()},
                    xPointer,
                    yPointer,
                    new int[]{op.x().majorStride()},
                    new int[]{op.y().majorStride()},

            };

            kernelParams = concate(kernelParams,convert(op.extraArgs(),getType(op)),new Object[]{result.pointer()});

            invokeFunction(op, kernelParams);
            setResultForOp(op);


        } else {
            //int n, int xOffset,double *dx,int incx,double result
            Object[] kernelParams = new Object[] {
                    new int[]{op.n()},
                    new int[]{op.x().offset()},
                    xPointer,
                    new int[]{op.x().majorStride()},

            };

            kernelParams = concate(kernelParams,convert(op.extraArgs(),getType(op)),new Object[]{result.pointer()});
            invokeFunction(op, kernelParams);
            setResultForOp(op);


        }

        result.destroy();
    }

    private Object[] concate(Object[]...arrs) {
        int length = 0;
        for(int i = 0; i < arrs.length; i++) {
            length += arrs[i].length;
        }

        Object[] ret = new Object[length];
        int count = 0;
        for(int i = 0; i < arrs.length; i++) {
            for(int j = 0; j < arrs[i].length; j++) {
                ret[count++] = arrs[i][j];
            }
        }

        return ret;
    }


    private void invokeFunction(Op op, Object... kernelParams) {
        String functionName = op instanceof TransformOp || op instanceof Accumulation ? op.name() + "_strided" : op.name();
        KernelFunctions.invoke(functionName,getType(op),kernelParams);

    }


    private void setResultForOp(Accumulation acc) {
        OpenclBuffer buff = (OpenclBuffer) acc.x().data();

        if (buff.dataType() == DataBuffer.DOUBLE) {
            double[] data = new double[1];
            data[0] = buff.buff().getDouble();
            acc.setCurrentResult(data[0]);
        }
        else {
            float[] data = new float[1];
            data[0] = buff.buff().getFloat();
            acc.setCurrentResult(data[0]);
        }
    }


    private void invoke(ScalarOp op) {
        OpenclBuffer xBuffer = (OpenclBuffer) op.x().data();
        Pointer xPointer = Pointer.to(xBuffer.buff()).withByteOffset(op.x().offset() * xBuffer.elementSize());

        OpenclBuffer zBuffer = (OpenclBuffer) op.z().data();
        Pointer zPointer = Pointer.to(zBuffer.buff()).withByteOffset(zBuffer.elementSize() * op.z().offset());

        if (op.y() != null) {
            OpenclBuffer yBuffer = (OpenclBuffer) op.y().data();
            Pointer yPointer = Pointer.to(yBuffer.buff()).withByteOffset(yBuffer.elementSize() * op.y().offset());
            Object[] kernelParams = new Object[]{
                    new int[]{op.n()},
                    new int[]{op.x().offset()},
                    new int[]{op.y().offset()},
                    xPointer,
                    yPointer,
                    new int[]{op.x().majorStride()},
                    new int[]{op.y().majorStride()},

            };

            kernelParams = concate(kernelParams,convert(op.extraArgs(),getType(op)),new Object[]{zPointer});
            invokeFunction(op, kernelParams);


        } else {
            //int n,int idx,double *dy,int incy,double *result
            //int n, int idx,double dx,double *dy,int incy,double *result

            Object[] kernelParams = new Object[] {
                    new int[]{op.n()},
                    new int[]{op.x().offset()},
                    PointerUtil.getPointer(op),
                    xPointer,
                    new int[]{op.x().majorStride()},

            };

            kernelParams = concate(kernelParams,convert(op.extraArgs(),getType(op)),new Object[]{zPointer});
            invokeFunction(op, kernelParams);


        }

    }


    private String getType(Op op) {
        return op.x().data().dataType() == DataBuffer.DOUBLE ? "double" : "float";
    }


    private void invoke(TransformOp op) {
        OpenclBuffer xBuffer = (OpenclBuffer) op.x().data();
        Pointer xPointer = Pointer.to(xBuffer.buff()).withByteOffset(xBuffer.elementSize() * op.x().offset());

        OpenclBuffer zBuffer = (OpenclBuffer) op.z().data();
        Pointer zPointer = Pointer.to(zBuffer.buff()).withByteOffset(zBuffer.elementSize() * op.z().offset());

        if (op.y() != null) {
            OpenclBuffer yBuffer = (OpenclBuffer) op.y().data();
            Pointer yPointer = Pointer.to(yBuffer.buff()).withByteOffset(op.y().offset() * yBuffer.elementSize());
            /**
             * Construct pointer arguments in the following order:
             * n
             * offset,
             * pointer to buffer
             * increment,
             * extraArgs,
             * result
             */
            Object[] params = new Object[9];
            params[0] = new int[]{op.n()};
            params[1] = new int[]{op.x().offset()};
            params[2] = new int[]{op.y().offset()};
            params[3] = xPointer;
            params[4] = yPointer;
            params[5] = new int[]{op.x().majorStride()};
            params[6] = new int[]{op.y().majorStride()};
            params = concate(params, convert(op.extraArgs(), getType(op)), new Object[]{zPointer});

            invokeFunction(op, params);


        } else {
            //int n,int idx,double *dy,int incy,double *result
            Object[] kernelParams = new Object[]{
                    new int[]{op.n()},
                    new int[]{op.x().offset()},
                    xPointer,
                    new int[]{op.x().majorStride()},

            };

            kernelParams = concate(kernelParams,convert(op.extraArgs(),getType(op)),new Object[]{zPointer});

            invokeFunction(op, kernelParams);


        }

    }


}


