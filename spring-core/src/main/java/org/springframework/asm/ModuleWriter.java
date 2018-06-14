/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.springframework.asm;

/**
 * @author Remi Forax
 */
final class ModuleWriter extends ModuleVisitor {
    /**
     * The class writer to which this Module attribute must be added.
     */
    private final ClassWriter cw;
    
    /**
     * size in byte of the Module attribute.
     */
    int size;
    
    /**
     * Number of attributes associated with the current module
     * (Version, ConcealPackages, etc) 
     */
    int attributeCount;
    
    /**
     * Size in bytes of the attributes associated with the current module
     */
    int attributesSize;
    
    /**
     * module name index in the constant pool
     */
    private final int name;
    
    /**
     * module access flags
     */
    private final int access;
    
    /**
     * module version index in the constant pool or 0
     */
    private final int version;
    
    /**
     * module main class index in the constant pool or 0
     */
    private int mainClass;
    
    /**
     * number of packages
     */
    private int packageCount;
    
    /**
     * The packages in bytecode form. This byte vector only contains
     * the items themselves, the number of items is store in packageCount
     */
    private ByteVector packages;
    
    /**
     * number of requires items
     */
    private int requireCount;
    
    /**
     * The requires items in bytecode form. This byte vector only contains
     * the items themselves, the number of items is store in requireCount
     */
    private ByteVector requires;
    
    /**
     * number of exports items
     */
    private int exportCount;
    
    /**
     * The exports items in bytecode form. This byte vector only contains
     * the items themselves, the number of items is store in exportCount
     */
    private ByteVector exports;
    
    /**
     * number of opens items
     */
    private int openCount;
    
    /**
     * The opens items in bytecode form. This byte vector only contains
     * the items themselves, the number of items is store in openCount
     */
    private ByteVector opens;
    
    /**
     * number of uses items
     */
    private int useCount;
    
    /**
     * The uses items in bytecode form. This byte vector only contains
     * the items themselves, the number of items is store in useCount
     */
    private ByteVector uses;
    
    /**
     * number of provides items
     */
    private int provideCount;
    
    /**
     * The uses provides in bytecode form. This byte vector only contains
     * the items themselves, the number of items is store in provideCount
     */
    private ByteVector provides;
    
    ModuleWriter(final ClassWriter cw, final int name,
            final int access, final int version) {
        super(Opcodes.ASM6);
        this.cw = cw;
        this.size = 16;  // name + access + version + 5 counts
        this.name = name;
        this.access = access;
        this.version = version;
    }
    
    @Override
    public void visitMainClass(String mainClass) {
        if (this.mainClass == 0) { // protect against several calls to visitMainClass
            this.cw.newUTF8("ModuleMainClass");
            this.attributeCount++;
            this.attributesSize += 8;
        }
        this.mainClass = this.cw.newClass(mainClass);
    }
    
    @Override
    public void visitPackage(String packaze) {
        if (this.packages == null) { 
            // protect against several calls to visitPackage
            this.cw.newUTF8("ModulePackages");
            this.packages = new ByteVector();
            this.attributeCount++;
            this.attributesSize += 8;
        }
        this.packages.putShort(this.cw.newPackage(packaze));
        this.packageCount++;
        this.attributesSize += 2;
    }
    
    @Override
    public void visitRequire(String module, int access, String version) {
        if (this.requires == null) {
            this.requires = new ByteVector();
        }
        this.requires.putShort(this.cw.newModule(module))
                .putShort(access)
                .putShort(version == null? 0: this.cw.newUTF8(version));
        this.requireCount++;
        this.size += 6;
    }
    
    @Override
    public void visitExport(String packaze, int access, String... modules) {
        if (this.exports == null) {
            this.exports = new ByteVector();
        }
        this.exports.putShort(this.cw.newPackage(packaze)).putShort(access);
        if (modules == null) {
            this.exports.putShort(0);
            this.size += 6;
        } else {
            this.exports.putShort(modules.length);
            for(String module: modules) {
                this.exports.putShort(this.cw.newModule(module));
            }    
            this.size += 6 + 2 * modules.length; 
        }
        this.exportCount++;
    }
    
    @Override
    public void visitOpen(String packaze, int access, String... modules) {
        if (this.opens == null) {
            this.opens = new ByteVector();
        }
        this.opens.putShort(this.cw.newPackage(packaze)).putShort(access);
        if (modules == null) {
            this.opens.putShort(0);
            this.size += 6;
        } else {
            this.opens.putShort(modules.length);
            for(String module: modules) {
                this.opens.putShort(this.cw.newModule(module));
            }    
            this.size += 6 + 2 * modules.length; 
        }
        this.openCount++;
    }
    
    @Override
    public void visitUse(String service) {
        if (this.uses == null) {
            this.uses = new ByteVector();
        }
        this.uses.putShort(this.cw.newClass(service));
        this.useCount++;
        this.size += 2;
    }
    
    @Override
    public void visitProvide(String service, String... providers) {
        if (this.provides == null) {
            this.provides = new ByteVector();
        }
        this.provides.putShort(this.cw.newClass(service));
        this.provides.putShort(providers.length);
        for(String provider: providers) {
            this.provides.putShort(this.cw.newClass(provider));
        }
        this.provideCount++;
        this.size += 4 + 2 * providers.length; 
    }
    
    @Override
    public void visitEnd() {
        // empty
    }

    void putAttributes(ByteVector out) {
        if (this.mainClass != 0) {
            out.putShort(this.cw.newUTF8("ModuleMainClass")).putInt(2).putShort(this.mainClass);
        }
        if (this.packages != null) {
            out.putShort(this.cw.newUTF8("ModulePackages"))
               .putInt(2 + 2 * this.packageCount)
               .putShort(this.packageCount)
               .putByteArray(this.packages.data, 0, this.packages.length);
        }
    }

    void put(ByteVector out) {
        out.putInt(this.size);
        out.putShort(this.name).putShort(this.access).putShort(this.version);
        out.putShort(this.requireCount);
        if (this.requires != null) {
            out.putByteArray(this.requires.data, 0, this.requires.length);
        }
        out.putShort(this.exportCount);
        if (this.exports != null) {
            out.putByteArray(this.exports.data, 0, this.exports.length);
        }
        out.putShort(this.openCount);
        if (this.opens != null) {
            out.putByteArray(this.opens.data, 0, this.opens.length);
        }
        out.putShort(this.useCount);
        if (this.uses != null) {
            out.putByteArray(this.uses.data, 0, this.uses.length);
        }
        out.putShort(this.provideCount);
        if (this.provides != null) {
            out.putByteArray(this.provides.data, 0, this.provides.length);
        }
    }    
}
