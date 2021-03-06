package sekwah.mods.narutomod.common.entity.specials;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

public class EntityMovingBlock extends Entity implements IEntityAdditionalSpawnData {

    private boolean canMove = true;

    /*public int blockID;
    public int metadata;*/

    public double toPosX;
    public double toPosY;
    public double toPosZ;

    public int aliveTicks;

    public int health;

    public NBTTagCompound fallingBlockTileEntityData;

    private Block block;
    private byte data;

    public boolean spawningParticles = false;
    private boolean hasDoneBlockCheck = false;

    private int lastSoundLoc = 0;

    public EntityMovingBlock(World world) {
        super(world);
        this.ignoreFrustumCheck = true;
        this.commonSetup();
        this.aliveTicks = 0;
    }

    public EntityMovingBlock(World par1World, double x, double y, double z, int blockID, int blockMetaID, int health) {
        super(par1World);
        this.block = Block.getBlockById(blockID);
        this.data = (byte) blockMetaID;
        this.health = health;
        this.commonSetup();
        this.preventEntitySpawning = true;
        this.toPosX = x + 0.5f;
        this.toPosY = y;
        this.toPosZ = z + 0.5f;
        this.forceSetPosition(x + 0.5f, y, z + 0.5f);
        this.canMove = false;
        this.aliveTicks = 0;
    }

    public void forceSetPosition(double posX, double posY, double posZ)
    {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.setBoundingBox(posX, posY, posZ);
    }

    public void setBoundingBox(double posX, double posY, double posZ) {
        float f = this.width / 2.0F;
        float f1 = this.height;
        this.boundingBox.setBounds(posX - (double)f, posY - (double)this.yOffset + (double)this.yOffset2, posZ - (double)f, posX + (double)f, posY - (double)this.yOffset + (double)this.yOffset2 + (double)f1, posZ + (double)f);
    }

    @Override
    public void setPosition(double posX, double posY, double posZ)
    {
        if(this.canMove) {
            this.forceSetPosition(posX, posY, posZ);
        }
    }

    @Override
    protected void entityInit() {
        this.dataWatcher.addObject(10, Byte.valueOf((byte)0));
    }

    public boolean getShaking() {
        return this.getRenderFlag((byte)1);
    }

    private boolean getRenderFlag(byte b) {
        return (this.dataWatcher.getWatchableObjectByte(10) & b) > 0;
    }

    public void setShaking(boolean shouldShake) {
        this.setRenderFlag((byte)1, shouldShake);
    }

    public void setRenderFlag(byte flag, boolean bool) {
        byte currentFlags = this.dataWatcher.getWatchableObjectByte(10);
        if(bool) {
            this.dataWatcher.updateObject(10, (byte) (currentFlags | flag));
        }
        else {
            this.dataWatcher.updateObject(10, (byte) (currentFlags & ~(flag)));
        }
    }

    private void commonSetup() {
        this.setSize(1F, 1F);
        //this.yOffset = this.height / 2.0F;
    }

    public AxisAlignedBB getCollisionBox(Entity entity)
    {
        return this.boundingBox;
    }

    /**
     * returns the bounding box for this entity
     */
    public AxisAlignedBB getBoundingBox()
    {
        return this.boundingBox;
    }

    public boolean canBePushed()
    {
        return true;
    }

    public boolean canBeCollidedWith()
    {
        return true;
    }

    private double lastDownAmount = 0;

    @Override
    public void onUpdate() {

        this.aliveTicks++;

        double downAmount = (((5 + 2) / ((this.aliveTicks) * 0.4f)) - 1f);
        if(downAmount < 0) {
            downAmount = 0;
        }


        //this.setBoundingBox(this.posX, this.posY + downAmount, this.posZ);
        double moveAmount = lastDownAmount == 0 ? 0 : (lastDownAmount - downAmount);
        lastDownAmount = downAmount;

        // TODO Move entity bounding box  down but dont move the actual entity (check what does lighting and what effects other parts but could work)
        /*List entities = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.copy().offset(0,downAmount,0));
        for(Object entityObj : entities) {
            if(!(entityObj instanceof EntityMovingBlock)) {
                Entity entity = (Entity) entityObj;
                entity.moveEntity(0,moveAmount,0);
            }
        }*/

        //this.posY += (this.toPosY - this.posY) / 4f;
        //this.posY += 0.01;

        if(!this.worldObj.isRemote) {
            WorldServer worldserver = (WorldServer)this.worldObj;



            if(this.spawningParticles && downAmount != 0) {
                if((int) downAmount != lastSoundLoc) {
                    this.lastSoundLoc = (int) downAmount;
                    this.worldObj.playSoundAtEntity(this, this.block.stepSound.getDigResourcePath(), 1, 1);
                }
                worldserver.func_147487_a("cloud", this.posX + ((double)this.rand.nextFloat() - 0.5D) * (double)this.width, this.boundingBox.minY + 0.1D,
                        this.posZ + ((double)this.rand.nextFloat() - 0.5D) * (double)this.width ,10,0,0,0,0.05);
                worldserver.func_147487_a("blockcrack_" + Block.getIdFromBlock(block) + "_" + this.data,
                        this.posX + ((double)this.rand.nextFloat() - 0.5D) * (double)this.width, this.boundingBox.minY + 0.1D,
                        this.posZ + ((double)this.rand.nextFloat() - 0.5D) * (double)this.width ,10,0,0,0,1);
            }

            if(this.health <= 30 && !this.getShaking()) {
                this.setShaking();
            }
            if(this.health <= 30 && Math.random() < 0.1) {
                this.worldObj.playSoundAtEntity(this, this.block.stepSound.getDigResourcePath(), 0.2f, 0.5f);
                worldserver.func_147487_a("blockcrack_" + Block.getIdFromBlock(block) + "_" + this.data,
                        this.posX + ((double)this.rand.nextFloat() - 0.5D) * (double)this.width, this.boundingBox.minY + 0.1D,
                        this.posZ + ((double)this.rand.nextFloat() - 0.5D) * (double)this.width ,10,0,0,0,1);
            }
            if(this.health-- < 0) {
                this.worldObj.playSoundAtEntity(this, this.block.stepSound.getDigResourcePath(), 1, 1);
                for (int i = 0; i < 3; i++) {
                    worldserver.func_147487_a("blockcrack_" + Block.getIdFromBlock(block) + "_" + this.data,
                            this.posX + ((double)this.rand.nextFloat() - 0.5D) * (double)this.width, this.boundingBox.minY + 0.1D,
                            this.posZ + ((double)this.rand.nextFloat() - 0.5D) * (double)this.width ,10,0,0,0,1);
                }
                //worldserver.func_147487_a("blockcrack_" + Block.getIdFromBlock(block) + "_" + this.data, this.posX + ((double)this.rand.nextFloat() - 0.5D) * (double)this.width, this.boundingBox.minY + 0.1D, this.posZ + ((double)this.rand.nextFloat() - 0.5D) * (double)this.width, 0,0,0);
                this.setDead();
            }
        }
    }

    private void setShaking() {
        this.setRenderFlag((byte)1, true);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbtTagCompound) {
        this.block = Block.getBlockFromName(nbtTagCompound.getString("Block"));
        this.data = nbtTagCompound.getByte("Data");
        this.toPosX = nbtTagCompound.getDouble("ToPosX");
        this.toPosY = nbtTagCompound.getDouble("ToPosY");
        this.toPosZ = nbtTagCompound.getDouble("ToPosZ");
        this.spawningParticles = nbtTagCompound.getBoolean("ShouldBreak");
        this.health = nbtTagCompound.getInteger("Health");

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbtTagCompound) {
        nbtTagCompound.setString("Block", Block.blockRegistry.getNameForObject(this.block));
        nbtTagCompound.setByte("Data", this.data);
        nbtTagCompound.setDouble("ToPosX", this.toPosX);
        nbtTagCompound.setDouble("ToPosY", this.toPosY);
        nbtTagCompound.setDouble("ToPosZ", this.toPosZ);
        nbtTagCompound.setBoolean("ShouldBreak", this.spawningParticles);
        nbtTagCompound.setInteger("Health", this.health);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeInt(Block.getIdFromBlock(this.block));
        buffer.writeByte(this.data);
        buffer.writeDouble(this.toPosY);
        buffer.writeBoolean(this.spawningParticles);
        buffer.writeInt(this.health);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        this.block = Block.getBlockById(additionalData.readInt());
        this.data = additionalData.readByte();
        this.toPosY = additionalData.readDouble();
        this.spawningParticles = additionalData.readBoolean();
        this.health = additionalData.readInt();
        this.canMove = false;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount)
    {
        if(this.health > 30) {
            switch(source.damageType) {
                case "waterBullet":
                    this.health -= 60;
                case "onFire":
                    this.health -= 180;
                case "explosion":
                case "explosion.player":
                    this.health -= 280;
                default:
                    this.health -= 30;
            }

            if(this.health < 30) {
                this.health = 30;
            }
        }

        if (this.isEntityInvulnerable())
        {
            return false;
        }
        else
        {
            this.setBeenAttacked();
            return false;
        }
    }

    public Block getBlock() {
        return this.block;
    }

    public byte getMetaData() {
        return this.data;
    }
}
