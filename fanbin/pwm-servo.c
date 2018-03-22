#include "linux/kernel.h"
#include "linux/init.h"
#include "linux/module.h"
#include "linux/cdev.h"
#include "linux/fs.h"
#include "linux/errno.h"
#include "linux/device.h"
#include "linux/pwm.h"
#include "asm/current.h"
#include "asm/uaccess.h"
#include "linux/sched.h"

#define DEVNAME     "pwm-servo"

static struct cdev *demop = NULL;

static int major = 0;
static int minor = 0;
const int count = 2;

static struct class *cls = NULL;

static struct pwm_device *pwm_servo2 = NULL;
static struct pwm_device *pwm_servo3 = NULL;
static int pwm_value2 = 1000, pwm_value3 = 1000;

static int demo_open(struct inode *inode, struct file *filep){
    // get command and pid
    printk(KERN_INFO "(%s:pid=%d), %s : %s : %d\n", current->comm, current->pid, __FILE__, __func__, __LINE__);
    // get major and minor from inode
    printk(KERN_INFO "(major=%d, minor=%d), %s : %s : %d\n", imajor(inode), iminor(inode), __FILE__, __func__, __LINE__);
    return 0;
} 
static int demo_release(struct inode *inode, struct file *filep){
    // get command and pid
    printk(KERN_INFO "(%s:pid=%d), %s : %s : %d\n", current->comm, current->pid, __FILE__, __func__, __LINE__);
    // get major and minor from inode
    printk(KERN_INFO "(major=%d, minor=%d), %s : %s : %d\n", imajor(inode), iminor(inode), __FILE__, __func__, __LINE__);
    return 0;
}
static ssize_t demo_read(struct file *fp, char __user *buf, size_t size, loff_t *offset){
    struct inode *inode = fp->f_path.dentry->d_inode;
    // get command and pid
    printk(KERN_INFO "(%s:pid=%d), %s : %s : %d\n", current->comm, current->pid, __FILE__, __func__, __LINE__);
    // get major and minor from inode
    printk(KERN_INFO "(major=%d, minor=%d), %s : %s : %d\n", imajor(inode), iminor(inode), __FILE__, __func__, __LINE__);

    if(size >3)
    {
        size = 3;
    }
    if(copy_to_user(buf, &pwm_value2, 4))
    {
        return -EFAULT;
    }

    return size;
}
static ssize_t demo_write(struct file *fp, const char __user *buf, size_t size, loff_t *offset){
    struct inode *inode = fp->f_path.dentry->d_inode;
    int value = 0;
    // get command and pid
    printk(KERN_INFO "(%s:pid=%d), %s : %s : %d\n", current->comm, current->pid, __FILE__, __func__, __LINE__);
    // get major and minor from inode
    printk(KERN_INFO "(major=%d, minor=%d), %s : %s : %d\n", imajor(inode), iminor(inode), __FILE__, __func__, __LINE__);

    if(size > 3)
    {
        size = 3;
    }
    printk("--------------value = %d-------------------\n", value);
    if(copy_from_user(&value, buf, 4))
    {
        return -EFAULT;
    }
    printk("--------------value = %d-------------------\n", value);
    if( (value <= 2000) && (value >= 1000))
    {
        printk("--------------value = %d-------------------\n", value);
        pwm_value2 = value ;
        pwm_config(pwm_servo2, pwm_value2 * 1000, 20000000);
        pwm_enable(pwm_servo2);
    }
    return size;
}

static struct file_operations fops = {
    .owner = THIS_MODULE,
    .open = demo_open,
    .release = demo_release,
    .read = demo_read,
    .write = demo_write,
};

static int __init demo_init(void)
{
    dev_t devnum;
    int ret, i;
    struct device *devp = NULL;


    printk("-----------------------pwm servo init---------------------\n");
    
    // 1. alloc cdev obj
    demop = cdev_alloc();
    if(NULL == demop)
    {
        return -ENOMEM;
    }

    // 2. init cdev obj
    cdev_init(demop, &fops);
    ret = alloc_chrdev_region(&devnum, minor, count, DEVNAME);
    if(ret)
    {
        goto ERR_STEP;
    }
    major = MAJOR(devnum);

    // 3. register cdev obj
    ret = cdev_add(demop, devnum, count);
    if(ret){
        goto ERR_STEP1;
    }

    cls = class_create(THIS_MODULE, DEVNAME);
    if(IS_ERR(cls)){
        ret = PTR_ERR(cls);
        goto ERR_STEP1;
    }

    for(i = minor; i < (count + minor); i++){
        devp = device_create(cls, NULL, MKDEV(major, i), NULL, "%s%d", DEVNAME, i);
        if(IS_ERR(devp)){
            ret = PTR_ERR(devp);
            goto ERR_STEP2;
        }
    }

    pwm_servo2 = pwm_request(2, "pwm-servo");
    pwm_config(pwm_servo2, pwm_value2 * 1000, 20000000);
    pwm_enable(pwm_servo2);


    return 0;

ERR_STEP2:
    for(--i;i >= minor; i--){
        device_destroy(cls, MKDEV(major, i));
    }
    class_destroy(cls);

ERR_STEP1:
    unregister_chrdev_region(devnum, count);


ERR_STEP:
    cdev_del(demop);
    printk(KERN_INFO "(%s:pid=%d), %s : %s : %d ----fail.\n", current->comm, current->pid, __FILE__, __func__, __LINE__);
    return ret;
}

static void __exit demo_exit(void)
{
    printk("------------------------pwm servo ------------------------\n");
}

module_init(demo_init);
module_exit(demo_exit);

MODULE_AUTHOR("fan <fanbinjim@qq.com>");
MODULE_DESCRIPTION("Fan pwm servo driver");
MODULE_LICENSE("GPL");
