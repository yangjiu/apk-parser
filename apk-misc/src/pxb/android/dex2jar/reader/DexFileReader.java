/*
 * Copyright (c) 2009-2010 Panxiaobo
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pxb.android.dex2jar.reader;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pxb.android.dex2jar.DataIn;
import pxb.android.dex2jar.DataInImpl;
import pxb.android.dex2jar.Dex;
import pxb.android.dex2jar.Field;
import pxb.android.dex2jar.Method;
import pxb.android.dex2jar.Proto;
import pxb.android.dex2jar.visitors.DexAnnotationAble;
import pxb.android.dex2jar.visitors.DexClassVisitor;
import pxb.android.dex2jar.visitors.DexCodeVisitor;
import pxb.android.dex2jar.visitors.DexFieldVisitor;
import pxb.android.dex2jar.visitors.DexFileVisitor;

/**
 * 读取dex文件
 * 
 * @author Panxiaobo [pxb1988@126.com]
 * @version $Id$
 */
public class DexFileReader implements Dex {
    private static final Logger log = LoggerFactory.getLogger(DexFileReader.class);
    private int class_defs_off;

    private int class_defs_size;

    private boolean continueOnException = true;

    private int data_off;

    private int data_size;
    private int field_ids_off;
    private int field_ids_size;
    private DataIn in;
    private int method_ids_off;
    private int method_ids_size;

    private int proto_ids_off;
    private int proto_ids_size;
    private int string_ids_off;
    private int string_ids_size;
    private int type_ids_off;
    private int type_ids_size;

    /**
     * 
     * @param data
     * @param continueOnException
     *            发生异常的时候是否继续
     */
    public DexFileReader(byte[] data) {
        DataIn in = new DataInImpl(data);
        this.in = in;
        // 0x 64 65 78
        byte[] magic = in.readBytes(3);
        log.debug("magic:'{}'", new String(magic));
        // 0x 0A ?
        in.skip(1);
        // 0x30 33 35
        byte[] version = in.readBytes(3);
        log.debug("version:'{}'", new String(version));
        // 0x 00 ?
        in.readByte();

        int checksum = in.readIntx();
        log.debug("checksum:0x{}", Integer.toHexString(checksum));
        // signiture
        // in.skipBytes(20);
        // byte[] signature =
        in.skip(20);
        // log.debug("signature:0x{}", Hex.from(signature).encode().toString());
        int fileSize = in.readIntx();
        log.debug("fileSize:{}", fileSize);
        int headSize = in.readIntx();
        log.debug("headSize:{}", headSize);
        int x28h = in.readIntx();
        log.debug("x28h:{} (0x{})", x28h, Integer.toHexString(x28h));
        int link_size = in.readIntx();
        log.debug("link_size:{}", link_size);
        int link_off = in.readIntx();
        log.debug("link_off:{} (0x{})", link_off, Integer.toHexString(link_off));

        int mapOff = in.readIntx();
        log.debug("x34h:{}", mapOff);

        string_ids_size = in.readIntx();
        log.debug("string_ids_size:{}", string_ids_size);
        string_ids_off = in.readIntx();
        log.debug("string_ids_off:{} (0x{})", string_ids_off, Integer.toHexString(string_ids_off));
        type_ids_size = in.readIntx();
        log.debug("type_ids_size:{} (0x{})", type_ids_size, Integer.toHexString(type_ids_size));
        type_ids_off = in.readIntx();
        log.debug("type_ids_off:{} (0x{})", type_ids_off, Integer.toHexString(type_ids_off));

        proto_ids_size = in.readIntx();
        log.debug("proto_ids_size:{} (0x{})", proto_ids_size, Integer.toHexString(proto_ids_size));
        proto_ids_off = in.readIntx();
        log.debug("proto_ids_off:{} (0x{})", proto_ids_off, Integer.toHexString(proto_ids_off));

        field_ids_size = in.readIntx();
        log.debug("field_ids_size:{} (0x{})", field_ids_size, Integer.toHexString(field_ids_size));
        field_ids_off = in.readIntx();
        log.debug("field_ids_off:{} (0x{})", field_ids_off, Integer.toHexString(field_ids_off));
        method_ids_size = in.readIntx();
        log.debug("method_ids_size:{} (0x{})", method_ids_size, Integer.toHexString(method_ids_size));
        method_ids_off = in.readIntx();
        log.debug("method_ids_off:{} (0x{})", method_ids_off, Integer.toHexString(method_ids_off));

        class_defs_size = in.readIntx();
        log.debug("class_defs_size:{} (0x{})", class_defs_size, Integer.toHexString(class_defs_size));

        class_defs_off = in.readIntx();
        log.debug("class_defs_off:{} (0x{})", class_defs_off, Integer.toHexString(class_defs_off));

        data_size = in.readIntx();
        log.debug("data_size:{} (0x{})", data_size, Integer.toHexString(data_size));

        data_off = in.readIntx();
        log.debug("data_off:{} (0x{})", data_off, Integer.toHexString(data_off));
        log.debug("=======End Of Head========");

        // {
        // in.pushMove(mapOff);
        // int value = in.readShortx();
        // System.out.println(this.getString(value));
        // in.readShortx();
        // int size = in.readIntx();
        // int offset = in.readIntx();
        // in.pop();
        // }

    }

    public DexFileReader(File f) throws IOException {
        this(new BufferedInputStream(new FileInputStream(f)));
    }

    public DexFileReader(InputStream in) throws IOException {
        this(IOUtils.toByteArray(in));
    }

    public void accept(DexFileVisitor dv) {
        DataIn in = this.in;
        for (int cid = 0; cid < class_defs_size; cid++) {
            int idxOffset = this.class_defs_off + cid * 32;
            in.pushMove(idxOffset);
            try {
                acceptClass(dv);
            } catch (Exception e) {
                log.error("Fail on class", e);
                if (!continueOnException) {
                    throw new RuntimeException(e);
                } else {
                    e.printStackTrace();
                }
            }
            in.pop();
        }
        dv.visitEnd();
    }

    private void acceptClass(DexFileVisitor dv) {
        DataIn in = this.in;
        DexClassVisitor dcv;
        {
            int class_idx = in.readIntx();
            String className = this.getType(class_idx);
            int access_flags = in.readIntx();
            int superclass_idx = in.readIntx();
            String superClassName = superclass_idx == -1 ? null : this.getType(superclass_idx);
            // 获取接口
            String[] interfaceNames = null;
            {
                int interfaces_off = in.readIntx();
                if (interfaces_off != 0) {
                    in.pushMove(interfaces_off);
                    int size = in.readIntx();
                    interfaceNames = new String[size];
                    for (int i = 0; i < size; i++) {
                        interfaceNames[i] = getType(in.readShortx());
                    }
                    in.pop();
                }
            }
            dcv = dv.visit(access_flags, className, superClassName, interfaceNames);
        }
        if (dcv == null)// 不处理
        {
            return;
        }
        // 获取源文件
        {
            int source_file_idx = in.readIntx();
            if (source_file_idx != -1)
                dcv.visitSource(this.getString(source_file_idx));
        }
        // 获取注解
        Map<Integer, Integer> fieldAnnotationPositions = new HashMap<Integer, Integer>();
        Map<Integer, Integer> methodAnnotationPositions = new HashMap<Integer, Integer>();
        Map<Integer, Integer> paramAnnotationPositions = new HashMap<Integer, Integer>();
        {
            int annotations_off = in.readIntx();
            if (annotations_off != 0) {
                in.pushMove(annotations_off);
                int class_annotations_off = in.readIntx();
                if (class_annotations_off != 0) {
                    in.pushMove(class_annotations_off);
                    new DexAnnotationReader(this).accept(in, dcv);
                    in.pop();
                }

                int field_annotation_size = in.readIntx();
                int method_annotation_size = in.readIntx();
                int parameter_annotation_size = in.readIntx();
                for (int i = 0; i < field_annotation_size; i++) {
                    int field_idx = in.readIntx();
                    int field_annotations_offset = in.readIntx();
                    fieldAnnotationPositions.put(field_idx, field_annotations_offset);
                }
                for (int i = 0; i < method_annotation_size; i++) {
                    int method_idx = in.readIntx();
                    int method_annotation_offset = in.readIntx();
                    methodAnnotationPositions.put(method_idx, method_annotation_offset);
                }
                for (int i = 0; i < parameter_annotation_size; i++) {
                    int method_idx = in.readIntx();
                    int parameter_annotation_offset = in.readIntx();
                    paramAnnotationPositions.put(method_idx, parameter_annotation_offset);
                }
                in.pop();
            }
        }

        int class_data_off = in.readIntx();

        int static_values_off = in.readIntx();

        if (class_data_off != 0) {
            in.pushMove(class_data_off);
            int static_fields = (int) in.readUnsignedLeb128();
            int instance_fields = (int) in.readUnsignedLeb128();
            int direct_methods = (int) in.readUnsignedLeb128();
            int virtual_methods = (int) in.readUnsignedLeb128();
            {
                int lastIndex = 0;
                {
                    Object[] constant = null;
                    {
                        if (static_values_off != 0) {
                            in.pushMove(static_values_off);
                            int size = (int) in.readUnsignedLeb128();
                            constant = new Object[size];
                            for (int i = 0; i < size; i++) {
                                constant[i] = Constant.ReadConstant(this, in);
                            }
                            in.pop();
                        }
                    }
                    for (int i = 0; i < static_fields; i++) {
                        Object value = null;
                        if (constant != null && i < constant.length) {
                            value = constant[i];
                        }
                        lastIndex = visitField(lastIndex, dcv, fieldAnnotationPositions, value);
                    }
                }
                lastIndex = 0;
                for (int i = 0; i < instance_fields; i++) {
                    lastIndex = visitField(lastIndex, dcv, fieldAnnotationPositions, null);
                }
                lastIndex = 0;
                for (int i = 0; i < direct_methods; i++) {
                    lastIndex = visitMethod(lastIndex, dcv, methodAnnotationPositions, paramAnnotationPositions);
                }
                lastIndex = 0;
                for (int i = 0; i < virtual_methods; i++) {
                    lastIndex = visitMethod(lastIndex, dcv, methodAnnotationPositions, paramAnnotationPositions);
                }
            }
            in.pop();
        }
        dcv.visitEnd();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pxb.android.dex2jar.Dex#getField(int)
     */
    public Field getField(int id) {
        if (id >= this.field_ids_size || id < 0)
            throw new IllegalArgumentException("Id out of bound");
        DataIn in = this.in;
        int idxOffset = this.field_ids_off + id * 8;
        in.pushMove(idxOffset);
        try {
            return new Field(this, in);
        } finally {
            in.pop();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see pxb.android.dex2jar.Dex#getMethod(int)
     */
    public Method getMethod(int id) {
        if (id >= this.method_ids_size || id < 0)
            throw new IllegalArgumentException("Id out of bound");
        DataIn in = this.in;
        int idxOffset = this.method_ids_off + id * 8;
        in.pushMove(idxOffset);
        try {
            return new Method(this, in);
        } finally {
            in.pop();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see pxb.android.dex2jar.Dex#getProto(int)
     */
    public Proto getProto(int id) {
        if (id >= this.proto_ids_size || id < 0)
            throw new IllegalArgumentException("Id out of bound");
        DataIn in = this.in;
        int idxOffset = this.proto_ids_off + id * 12;
        in.pushMove(idxOffset);
        try {
            return new Proto(this, in);
        } finally {
            in.pop();
        }
    }

    /**
     * 一个String id为4字节
     * 
     * @see pxb.android.dex2jar.Dex#getString(int)
     */
    public String getString(int id) {
        if (id >= this.string_ids_size || id < 0)
            throw new IllegalArgumentException("Id out of bound");
        DataIn in = this.in;
        int idxOffset = this.string_ids_off + id * 4;
        in.pushMove(idxOffset);
        try {
            int offset = in.readIntx();
            in.pushMove(offset);
            try {
                int length = (int) in.readUnsignedLeb128();
                ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
                for (int b = in.readByte(); b != 0; b = in.readByte()) {
                    baos.write(b);
                }
                return new String(baos.toByteArray(), UTF8);
            } finally {
                in.pop();
            }
        } finally {
            in.pop();
        }
    }

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /*
     * (non-Javadoc)
     * 
     * @see pxb.android.dex2jar.Dex#getType(int)
     */
    public String getType(int id) {
        if (id >= this.type_ids_size || id < 0)
            throw new IllegalArgumentException("Id out of bound");
        DataIn in = this.in;
        int idxOffset = this.type_ids_off + id * 4;
        in.pushMove(idxOffset);
        try {
            int offset = in.readIntx();
            return this.getString(offset);
        } finally {
            in.pop();
        }
    }

    /**
     * 访问成员
     * 
     * @param lastIndex
     * @param dcv
     * @param fieldAnnotationPositions
     * @param value
     * @return
     */
    protected int visitField(int lastIndex, DexClassVisitor dcv, Map<Integer, Integer> fieldAnnotationPositions, Object value) {
        int diff = (int) in.readUnsignedLeb128();
        int field_id = lastIndex + diff;
        Field field = getField(field_id);
        int field_access_flags = (int) in.readUnsignedLeb128();
        field.setAccessFlags(field_access_flags);
        // //////////////////////////////////////////////////////////////
        DexFieldVisitor dfv = dcv.visitField(field, value);
        if (dfv != null) {
            Integer annotation_offset = fieldAnnotationPositions.get(field_id);
            if (annotation_offset != null) {
                in.pushMove(annotation_offset);
                new DexAnnotationReader(this).accept(in, dfv);
                in.pop();
            }
            dfv.visitEnd();
        }
        // //////////////////////////////////////////////////////////////
        return field_id;
    }

    /**
     * 访问方法
     * 
     * @param lastIndex
     * @param cv
     * @param methodAnnos
     * @param parameterAnnos
     * @return
     */
    protected int visitMethod(int lastIndex, DexClassVisitor cv, Map<Integer, Integer> methodAnnos, Map<Integer, Integer> parameterAnnos) {
        int diff = (int) in.readUnsignedLeb128();
        int method_id = lastIndex + diff;
        Method method = getMethod(method_id);
        int method_access_flags = (int) in.readUnsignedLeb128();
        int code_off = (int) in.readUnsignedLeb128();
        method.setAccessFlags(method_access_flags);
        pxb.android.dex2jar.visitors.DexMethodVisitor dmv = cv.visitMethod(method);
        if (dmv != null) {
            {
                Integer annotation_offset = methodAnnos.get(method_id);
                if (annotation_offset != null) {
                    in.pushMove(annotation_offset);
                    new DexAnnotationReader(this).accept(in, dmv);
                    in.pop();
                }
            }
            {
                Integer parameter_annotation_offset = parameterAnnos.get(method_id);
                if (parameter_annotation_offset != null) {
                    in.pushMove(parameter_annotation_offset);
                    int sizeJ = in.readIntx();
                    for (int j = 0; j < sizeJ; j++) {
                        int field_annotation_offset = in.readIntx();
                        in.pushMove(field_annotation_offset);
                        DexAnnotationAble dpav = dmv.visitParamesterAnnotation(j);
                        if (dpav != null)
                            new DexAnnotationReader(this).accept(in, dpav);
                        in.pop();
                    }
                    in.pop();
                }
            }
            if (code_off != 0) {
                in.pushMove(code_off);
                DexCodeVisitor dcv = dmv.visitCode();
                if (dcv != null) {
                    try {
                        new DexCodeReader(this, in, method).accept(dcv);
                    } catch (Exception e) {
                        throw new RuntimeException("Error in method:[" + method + "]", e);
                    }
                }
                in.pop();
            }
            dmv.visitEnd();
        }
        return method_id;
    }
}
