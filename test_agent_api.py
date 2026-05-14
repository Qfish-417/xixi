#!/usr/bin/env python3
"""
测试脚本：验证AI助手普通模式下的API调用
"""

import requests
import json
import time

# 配置
BASE_URL = "http://localhost:8081"
EMAIL = "2320283299@qq.com"
PASSWORD = "123123"

def test_login():
    """测试用户登录获取Token"""
    print("=== 测试1: 用户登录 ===")
    url = f"{BASE_URL}/api/auth/login"
    data = {"email": EMAIL, "password": PASSWORD}
    
    try:
        response = requests.post(url, json=data)
        response.raise_for_status()
        result = response.json()
        
        if "token" in result:
            print(f"✓ 登录成功")
            print(f"  Token: {result['token'][:30]}...")
            return result["token"]
        else:
            print(f"✗ 登录失败: {result}")
            return None
    except Exception as e:
        print(f"✗ 登录请求失败: {e}")
        return None

def test_agent_master(token, test_input):
    """测试智能体主接口"""
    print(f"\n=== 测试2: 调用智能体接口 ===")
    print(f"  输入: {test_input}")
    
    url = f"{BASE_URL}/api/agent/master"
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    data = {
        "input": test_input,
        "modelName": "deepseek"
    }
    
    try:
        response = requests.post(url, headers=headers, json=data, timeout=60)
        response.raise_for_status()
        result = response.json()
        
        print(f"✓ 请求成功")
        print(f"  响应: {json.dumps(result, ensure_ascii=False)}")
        
        # 检查是否为真实AI回答（不是模拟回答）
        message = result.get("message", "")
        if "模拟" in message or "这是一段由深度求索大模型生成的" in message:
            print("✗ 警告: 返回的是模拟回答，不是真实AI响应")
            return False
        else:
            print("✓ 成功: 返回的是真实AI回答")
            return True
            
    except requests.exceptions.Timeout:
        print("✗ 请求超时")
        return False
    except Exception as e:
        print(f"✗ 请求失败: {e}")
        if response:
            print(f"  响应状态码: {response.status_code}")
            print(f"  响应内容: {response.text}")
        return False

def main():
    print("=" * 60)
    print("AI助手普通模式测试脚本")
    print("=" * 60)
    
    # 测试1: 登录
    token = test_login()
    if not token:
        print("\n✗ 登录失败，无法继续测试")
        return
    
    # 测试2: 调用智能体接口 - 自我介绍
    success1 = test_agent_master(token, "介绍一下你自己")
    
    # 测试3: 调用智能体接口 - 问答
    success2 = test_agent_master(token, "什么是人工智能？")
    
    # 测试4: 调用智能体接口 - 创意写作
    success3 = test_agent_master(token, "写一首关于春天的诗")
    
    print("\n" + "=" * 60)
    print("测试总结")
    print("=" * 60)
    print(f"  测试1 (自我介绍): {'通过' if success1 else '失败'}")
    print(f"  测试2 (知识问答): {'通过' if success2 else '失败'}")
    print(f"  测试3 (创意写作): {'通过' if success3 else '失败'}")
    
    if success1 and success2 and success3:
        print("\n✓ 所有测试通过！AI助手可以正常调用DeepSeek模型生成真实回答")
    else:
        print("\n✗ 部分测试失败，请检查API密钥配置和网络连接")

if __name__ == "__main__":
    main()