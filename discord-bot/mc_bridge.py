import aiohttp
import os
import json

class MinecraftBridge:
    BASE_URL = os.getenv("MC_HTTP_ADDRESS")
    API_KEY = os.getenv("MC_API")

    @classmethod
    async def query(cls, command: str):
        print(cls.BASE_URL + "/command")
        headers = {"X-API-Key": cls.API_KEY}
        async with aiohttp.ClientSession() as session:
            async with session.post(cls.BASE_URL + "/command", data=command.encode(), headers=headers) as resp:
                print(f"Querying: {command} | Status: {resp.status}")
                if resp.status == 200:
                    return json.loads(await resp.text())
                return {"error": f"HTTP {resp.status}"}
