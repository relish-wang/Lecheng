package com.lechange.demo.business.entity;
/**
 * 文件描述：package com.android.business.entity;
 * 功能说明：
 * 版权申明：
 * @author ding_qili 
 * @version 2015-6-17下午3:29:12
 */

public class ChannelPTZInfo {
	
	public enum Operation{
		Move,//移动
		Locate,//定位
		Stop;//立即停止！
	}
	
	
	public enum Duration{//持续多久
		Forever,//永远
		Long,//长
		Generral,//普通
		Short;//短
	}
	
	public enum Direction{//方向
		Left,
		Right,
		Up,
		Down,
		ZoomIn,
		ZoomOut;
	}
	
	public ChannelPTZInfo(Operation operation,Direction direction) {
		this.operation = operation;
		this.direction = direction;
	}
	
	
	
	/** 操作行为；move表示移动，locate表示定位 */
	private Operation operation = Operation.Move;
	/**
	 * 持续时间
	 */
	private Duration duration = Duration.Generral;
	/**
	 * 方向
	 */
	private Direction direction = Direction.Left;
	/**
	 * @return the operation
	 */
	public Operation getOperation() {
		return operation;
	}
	/**
	 * @param operation the operation to set
	 */
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
	/**
	 * @return the duration
	 */
	public Duration getDuration() {
		return duration;
	}
	/**
	 * @param duration the duration to set
	 */
	public void setDuration(Duration duration) {
		this.duration = duration;
	}
	/**
	 * @return the direction
	 */
	public Direction getDirection() {
		return direction;
	}
	/**
	 * @param direction the direction to set
	 */
	public void setDirection(Direction direction) {
		this.direction = direction;
	}
	
	
	
}
