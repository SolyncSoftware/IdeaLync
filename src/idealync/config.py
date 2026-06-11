from dataclasses import dataclass

@dataclass
class IdeaLyncConfig:
    """General config for IdeaLync"""
    role_id: int # the role that all bots will have
