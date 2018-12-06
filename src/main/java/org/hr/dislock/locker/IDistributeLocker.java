/**
 * BBD Service Inc
 * All Rights Reserved @2018
 */
package org.hr.dislock.locker;

/**
 *
 * @author huangr
 * @version $Id: IDistributeLocker.java, v0.1 2018/12/6 16:09 huangr Exp $$
 */
public interface IDistributeLocker {

    boolean tryLock(String lockKey);

    /**
     * @param lockKey 锁定资源
     * @param timeout 超时时间（毫秒）
     * @return
     */
    boolean tryLock(String lockKey, long timeout);

    /**释放资源
     * @param lockKey
     */
    void unlock(String lockKey);
}
